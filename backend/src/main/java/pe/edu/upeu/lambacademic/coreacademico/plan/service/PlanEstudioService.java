package pe.edu.upeu.lambacademic.coreacademico.plan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.service.InstitucionalService;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResumen;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;
import pe.edu.upeu.lambacademic.coreacademico.curso.mapper.CursoMapper;
import pe.edu.upeu.lambacademic.coreacademico.curso.repository.CursoRepository;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanDistribucionResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioRequest;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanResumenResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.DetallePlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.PlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.repository.DetallePlanEstudioRepository;
import pe.edu.upeu.lambacademic.coreacademico.plan.repository.PlanEstudioRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * OPERACION CABECERA-DETALLE de la malla curricular (origen: LP2_G1, Cristian).
 * crear() es @Transactional: si cualquier detalle viola una regla, se lanza
 * ReglaNegocioException y no queda ni la cabecera ni ningun detalle (rollback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanEstudioService {

    /** Limites de creditos por ciclo (regla 2). */
    static final int CREDITOS_MIN_CICLO = 6;
    static final int CREDITOS_MAX_CICLO = 24;

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("anio", "codigo", "nombre", "totalCreditos", "totalCursos", "version", "id");

    private final PlanEstudioRepository planRepository;
    private final DetallePlanEstudioRepository detalleRepository;
    private final CursoRepository cursoRepository;
    private final CursoMapper cursoMapper;
    private final InstitucionalService institucionalService;

    @Transactional
    public PlanEstudioResponse crear(PlanEstudioRequest request) {
        if (planRepository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException("Ya existe un plan con codigo " + request.codigo());
        }
        EscuelaResponse escuela = institucionalService.obtenerEscuela(request.idEscuela());
        if (!Boolean.TRUE.equals(escuela.activo())) {
            throw new ReglaNegocioException("La escuela " + escuela.codigo() + " esta inactiva");
        }

        PlanEstudio plan = PlanEstudio.builder()
                .escuelaId(escuela.id())
                .codigo(request.codigo())
                .nombre(request.nombre())
                .anio(request.anio())
                .version(1)
                .estado(PlanEstudio.Estado.VIGENTE)
                .detalles(new ArrayList<>())
                .build();

        Map<Long, Integer> cicloPorCurso = new HashMap<>();
        Set<Long> electivos = new HashSet<>();
        Map<Integer, Integer> creditosPorCiclo = new TreeMap<>();
        for (PlanEstudioRequest.DetallePlanRequest d : request.detalles()) {
            // Regla 1: un curso no se repite dentro del mismo plan
            if (cicloPorCurso.put(d.idCurso(), d.ciclo()) != null) {
                throw new ReglaNegocioException("El curso " + d.idCurso() + " esta repetido en la malla (regla 1)");
            }
            creditosPorCiclo.merge(d.ciclo(), d.creditos(), Integer::sum);
            if (d.tipo() == DetallePlanEstudio.Tipo.ELECTIVO) {
                electivos.add(d.idCurso());
            }
        }

        for (PlanEstudioRequest.DetallePlanRequest d : request.detalles()) {
            Curso curso = cursoActivo(d.idCurso());
            Curso prerrequisito = null;
            if (d.idPrerrequisito() != null) {
                validarPrerrequisito(d, cicloPorCurso, electivos);
                prerrequisito = cursoActivo(d.idPrerrequisito());
            }
            plan.addDetalle(DetallePlanEstudio.builder()
                    .curso(curso)
                    .ciclo(d.ciclo())
                    .creditos(d.creditos())
                    .tipo(d.tipo() == null ? DetallePlanEstudio.Tipo.OBLIGATORIO : d.tipo())
                    .prerrequisito(prerrequisito)
                    .build());
        }

        // Regla 2: rango de creditos por ciclo
        creditosPorCiclo.forEach((ciclo, creditos) -> {
            if (creditos < CREDITOS_MIN_CICLO || creditos > CREDITOS_MAX_CICLO) {
                throw new ReglaNegocioException("El ciclo " + ciclo + " tiene " + creditos
                        + " creditos; debe estar entre " + CREDITOS_MIN_CICLO + " y " + CREDITOS_MAX_CICLO
                        + " (regla 2)");
            }
        });

        plan.recalcularTotales();
        PlanEstudio guardado = planRepository.save(plan);
        log.info("Plan de estudios creado id={} codigo={} cursos={} creditos={}", guardado.getId(),
                guardado.getCodigo(), guardado.getTotalCursos(), guardado.getTotalCreditos());
        return toResponse(guardado, escuela.nombre());
    }

    @Transactional(readOnly = true)
    public PlanEstudioResponse obtener(Long id) {
        PlanEstudio plan = buscarOFallar(id);
        return toResponse(plan, nombresDeEscuelas().get(plan.getEscuelaId()));
    }

    @Transactional(readOnly = true)
    public List<PlanEstudioResponse> buscar(Long idEscuela, PlanEstudio.Estado estado, Integer anio, String texto,
                                            Integer creditosMin, Integer creditosMax, String sortBy, String dir) {
        Sort.Direction direccion = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "anio";
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
        Map<Long, String> escuelas = nombresDeEscuelas();
        return planRepository.buscar(idEscuela, estado, anio, patron, creditosMin, creditosMax,
                        Sort.by(direccion, campo))
                .stream().map(p -> toResponse(p, escuelas.get(p.getEscuelaId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanDistribucionResponse> distribucion(Long id) {
        if (!planRepository.existsById(id)) {
            throw new ResourceNotFoundException("Plan de estudios", id);
        }
        return planRepository.distribucionPorCiclo(id).stream()
                .map(f -> new PlanDistribucionResponse(((Number) f[0]).intValue(),
                        ((Number) f[1]).longValue(), ((Number) f[2]).longValue()))
                .toList();
    }

    /** Regla 4: un plan OBSOLETO no se edita; se genera una nueva version VIGENTE. */
    @Transactional
    public PlanEstudioResponse versionar(Long id) {
        PlanEstudio actual = buscarOFallar(id);
        if (actual.getEstado() == PlanEstudio.Estado.OBSOLETO) {
            throw new ReglaNegocioException("El plan esta OBSOLETO: no se puede versionar (regla 4)");
        }
        int nuevaVersion = actual.getVersion() + 1;
        String base = actual.getCodigo().replaceFirst("-V\\d+$", "");
        String codigo = base + "-V" + nuevaVersion;
        if (codigo.length() > 30) {
            throw new ReglaNegocioException("El codigo de la nueva version supera 30 caracteres: " + codigo);
        }
        if (planRepository.existsByCodigo(codigo)) {
            throw new ReglaNegocioException("Ya existe un plan con codigo " + codigo);
        }
        actual.setEstado(PlanEstudio.Estado.OBSOLETO);
        PlanEstudio nueva = PlanEstudio.builder()
                .escuelaId(actual.getEscuelaId())
                .codigo(codigo)
                .nombre(actual.getNombre())
                .anio(actual.getAnio())
                .version(nuevaVersion)
                .estado(PlanEstudio.Estado.VIGENTE)
                .detalles(new ArrayList<>())
                .build();
        actual.getDetalles().forEach(d -> nueva.addDetalle(DetallePlanEstudio.builder()
                .curso(d.getCurso())
                .ciclo(d.getCiclo())
                .creditos(d.getCreditos())
                .tipo(d.getTipo())
                .prerrequisito(d.getPrerrequisito())
                .build()));
        nueva.recalcularTotales();
        planRepository.save(actual);
        PlanEstudio guardada = planRepository.save(nueva);
        log.info("Plan {} versionado -> {} ({})", id, guardada.getId(), guardada.getCodigo());
        return toResponse(guardada, nombresDeEscuelas().get(guardada.getEscuelaId()));
    }

    @Transactional(readOnly = true)
    public List<PlanResumenResponse> resumen() {
        Map<Long, String> escuelas = nombresDeEscuelas();
        return planRepository.resumenPorEscuela().stream()
                .map(f -> {
                    Long idEscuela = ((Number) f[0]).longValue();
                    return new PlanResumenResponse(idEscuela, escuelas.get(idEscuela), String.valueOf(f[1]),
                            ((Number) f[2]).longValue(), ((Number) f[3]).longValue());
                })
                .toList();
    }

    /* ---------------------- API publica para Matricula ---------------------- */

    /** Prerrequisitos del curso segun los planes vigentes de la escuela (o de todas si es null). */
    @Transactional(readOnly = true)
    public List<CursoResumen> prerrequisitosDeCurso(Long cursoId, Long escuelaId) {
        return detalleRepository.prerrequisitosDeCurso(cursoId, escuelaId, PlanEstudio.Estado.VIGENTE)
                .stream().map(cursoMapper::toResumen).toList();
    }

    /* -------------------------------- reglas -------------------------------- */

    private void validarPrerrequisito(PlanEstudioRequest.DetallePlanRequest d, Map<Long, Integer> cicloPorCurso,
                                      Set<Long> electivos) {
        if (d.idPrerrequisito().equals(d.idCurso())) {
            throw new ReglaNegocioException("El curso " + d.idCurso() + " no puede ser prerrequisito de si mismo");
        }
        Integer cicloPrerrequisito = cicloPorCurso.get(d.idPrerrequisito());
        if (cicloPrerrequisito == null) {
            throw new ReglaNegocioException("El prerrequisito " + d.idPrerrequisito()
                    + " del curso " + d.idCurso() + " debe pertenecer al mismo plan");
        }
        if (cicloPrerrequisito >= d.ciclo()) {
            throw new ReglaNegocioException("El prerrequisito " + d.idPrerrequisito()
                    + " debe estar en un ciclo anterior al del curso " + d.idCurso());
        }
        // Regla 3: un curso electivo no puede ser prerrequisito de otro
        if (electivos.contains(d.idPrerrequisito())) {
            throw new ReglaNegocioException("Un curso electivo no puede ser prerrequisito (regla 3)");
        }
    }

    private Curso cursoActivo(Long idCurso) {
        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", idCurso));
        if (!Boolean.TRUE.equals(curso.getActivo())) {
            throw new ReglaNegocioException("El curso " + curso.getCodigo() + " esta inactivo");
        }
        return curso;
    }

    private Map<Long, String> nombresDeEscuelas() {
        return institucionalService.listarEscuelas(null).stream()
                .collect(Collectors.toMap(EscuelaResponse::id, EscuelaResponse::nombre, (a, b) -> a));
    }

    private PlanEstudio buscarOFallar(Long id) {
        return planRepository.findDetalladoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudios", id));
    }

    private PlanEstudioResponse toResponse(PlanEstudio p, String escuelaNombre) {
        List<PlanEstudioResponse.DetallePlanResponse> detalles = p.getDetalles().stream()
                .map(d -> new PlanEstudioResponse.DetallePlanResponse(
                        d.getId(), d.getCurso().getId(), d.getCurso().getCodigo(), d.getCurso().getNombre(),
                        d.getCiclo(), d.getCreditos(), d.getTipo(),
                        d.getPrerrequisito() == null ? null : d.getPrerrequisito().getId(),
                        d.getPrerrequisito() == null ? null : d.getPrerrequisito().getCodigo()))
                .toList();
        return new PlanEstudioResponse(p.getId(), p.getEscuelaId(), escuelaNombre, p.getCodigo(), p.getNombre(),
                p.getAnio(), p.getVersion(), p.getEstado(), p.getTotalCreditos(), p.getTotalCursos(), detalles);
    }
}
