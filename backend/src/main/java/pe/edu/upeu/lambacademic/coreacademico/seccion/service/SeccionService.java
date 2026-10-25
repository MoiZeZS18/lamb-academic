package pe.edu.upeu.lambacademic.coreacademico.seccion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;
import pe.edu.upeu.lambacademic.coreacademico.curso.repository.CursoRepository;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.HorarioAulaRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionActualizacionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionMatriculaInfo;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionOcupacionReporte;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionResponse;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.HorarioAula;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.Seccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.mapper.SeccionMapper;
import pe.edu.upeu.lambacademic.coreacademico.seccion.port.PeriodoAcademicoPort;
import pe.edu.upeu.lambacademic.coreacademico.seccion.port.SeccionEnUsoPort;
import pe.edu.upeu.lambacademic.coreacademico.seccion.repository.SeccionRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Programacion de secciones (oferta academica): seccion + horarios en una transaccion,
 * con validacion de periodo, docente, cruces de aula y de docente. Tambien es la API
 * publica que Matricula usa para consultar secciones y reservar cupos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeccionService {

    /** Franja institucional de clases. */
    static final LocalTime HORA_MINIMA = LocalTime.of(7, 0);
    static final LocalTime HORA_MAXIMA = LocalTime.of(22, 0);

    private static final Set<String> CAMPOS_ORDENABLES = Set.of("grupo", "periodoId", "limiteVacantes", "id");

    private final SeccionRepository seccionRepository;
    private final CursoRepository cursoRepository;
    private final SeccionMapper mapper;
    private final PersonaService personaService;
    private final PeriodoAcademicoPort periodoAcademicoPort;
    private final SeccionEnUsoPort seccionEnUsoPort;

    @Transactional
    public SeccionResponse programar(SeccionRequest request) {
        Curso curso = cursoRepository.findById(request.cursoId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso", request.cursoId()));
        if (!Boolean.TRUE.equals(curso.getActivo())) {
            throw new ReglaNegocioException("El curso " + curso.getCodigo() + " esta inactivo");
        }
        validarPeriodo(request.periodoId());
        if (seccionRepository.existsByPeriodoIdAndCursoIdAndGrupo(request.periodoId(), curso.getId(), request.grupo())) {
            throw new ReglaNegocioException("Ya existe el grupo " + request.grupo() + " del curso "
                    + curso.getCodigo() + " en el periodo " + request.periodoId());
        }
        PersonaResponse docente = request.docenteId() == null ? null : personaService.obtenerDocente(request.docenteId());

        Seccion seccion = new Seccion();
        seccion.setCurso(curso);
        seccion.setPeriodoId(request.periodoId());
        seccion.setGrupo(request.grupo());
        seccion.setDocenteId(request.docenteId());
        seccion.setLimiteVacantes(request.limiteVacantes());
        seccion.setCuposOcupados(0);
        seccion.setEstado(EstadoSeccion.ABIERTA);

        List<HorarioAula> horarios = new ArrayList<>();
        for (HorarioAulaRequest horarioRequest : request.horarios()) {
            HorarioAula horario = mapper.toEntity(horarioRequest);
            validarHorario(horario, horarios, seccion, null);
            horarios.add(horario);
            seccion.agregarHorario(horario);
        }

        Seccion guardada = seccionRepository.save(seccion);
        log.info("Seccion programada id={} curso={} grupo={} periodo={} horarios={}", guardada.getId(),
                curso.getCodigo(), guardada.getGrupo(), guardada.getPeriodoId(), horarios.size());
        return mapper.toResponse(guardada, docente == null ? null : docente.nombreCompleto());
    }

    @Transactional
    public SeccionResponse actualizar(Long id, SeccionActualizacionRequest request) {
        Seccion seccion = buscarOFallar(id);
        if (seccion.getEstado() == EstadoSeccion.CANCELADA) {
            throw new ReglaNegocioException("La seccion " + id + " esta cancelada");
        }
        if (request.limiteVacantes() < seccion.getCuposOcupados()) {
            throw new ReglaNegocioException("El limite de vacantes (" + request.limiteVacantes()
                    + ") no puede ser menor que los cupos ya ocupados (" + seccion.getCuposOcupados() + ")");
        }
        PersonaResponse docente = null;
        if (request.docenteId() != null) {
            docente = personaService.obtenerDocente(request.docenteId());
            for (HorarioAula horario : seccion.getHorarios()) {
                validarCruceDocente(seccion.getPeriodoId(), id, request.docenteId(), horario);
            }
        }
        seccion.setDocenteId(request.docenteId());
        seccion.setLimiteVacantes(request.limiteVacantes());
        Seccion guardada = seccionRepository.save(seccion);
        return mapper.toResponse(guardada, docente == null ? null : docente.nombreCompleto());
    }

    @Transactional
    public SeccionResponse cambiarEstado(Long id, EstadoSeccion nuevo) {
        Seccion seccion = buscarOFallar(id);
        if (seccion.getEstado() == EstadoSeccion.CANCELADA) {
            throw new ReglaNegocioException("Una seccion cancelada no puede cambiar de estado");
        }
        if (nuevo == EstadoSeccion.CANCELADA && seccion.getCuposOcupados() > 0) {
            throw new ReglaNegocioException("No se puede cancelar la seccion " + id + ": tiene "
                    + seccion.getCuposOcupados() + " matriculados");
        }
        log.info("Seccion id={} cambia de estado {} -> {}", id, seccion.getEstado(), nuevo);
        seccion.setEstado(nuevo);
        return toResponse(seccionRepository.save(seccion));
    }

    /**
     * [U2, S8] Elimina una seccion que nunca se uso: sin cupos ocupados, sin detalles de matricula
     * (ni siquiera anulados, que conservan su historia) y en un periodo no cerrado. Si ya se uso,
     * la operacion correcta es cancelarla (PATCH /estado), no borrarla.
     */
    @Transactional
    public void eliminar(Long id) {
        Seccion seccion = buscarOFallar(id);
        if (seccion.getCuposOcupados() > 0 || seccionEnUsoPort.tieneMatriculas(id)) {
            throw new ReglaNegocioException("No se puede eliminar la seccion " + id
                    + ": ya tiene matriculas registradas. Cancelala en su lugar");
        }
        validarPeriodo(seccion.getPeriodoId());
        seccionRepository.delete(seccion);
        log.info("Seccion eliminada id={} curso={} grupo={}", id, seccion.getCurso().getCodigo(), seccion.getGrupo());
    }

    @Transactional(readOnly = true)
    public List<SeccionResponse> buscar(Long periodoId, Long cursoId, Long docenteId, EstadoSeccion estado,
                                        boolean soloConVacantes, String sortBy, String dir) {
        Sort.Direction direccion = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "grupo";
        Sort sort = Sort.by(direccion, campo).and(Sort.by("id"));
        List<Seccion> secciones = seccionRepository.buscar(periodoId, cursoId, docenteId, estado,
                soloConVacantes ? 1 : 0, sort);
        Map<Long, String> docentes = nombresDeDocentes();
        return secciones.stream()
                .map(s -> mapper.toResponse(s, s.getDocenteId() == null ? null : docentes.get(s.getDocenteId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SeccionResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional(readOnly = true)
    public List<SeccionOcupacionReporte> ocupacionPorCurso(Long periodoId) {
        validarPeriodoExiste(periodoId);
        return seccionRepository.ocupacionPorCurso(periodoId, EstadoSeccion.CANCELADA);
    }

    /* ------------------------- API publica para Matricula ------------------------- */

    @Transactional(readOnly = true)
    public Optional<SeccionMatriculaInfo> obtenerParaMatricula(Long seccionId) {
        return seccionRepository.findDetalladaById(seccionId).map(mapper::toMatriculaInfo);
    }

    /**
     * Ocupa un cupo de forma atomica (se une a la transaccion de la matricula: si la
     * matricula falla despues, el cupo se libera con el rollback).
     */
    @Transactional
    public void reservarCupo(Long seccionId) {
        if (seccionRepository.reservarCupo(seccionId) == 0) {
            Seccion seccion = buscarOFallar(seccionId);
            if (seccion.getEstado() != EstadoSeccion.ABIERTA) {
                throw new ReglaNegocioException("La seccion " + seccionId + " no esta abierta para matricula");
            }
            throw new ReglaNegocioException("La seccion " + seccionId + " no tiene cupos disponibles");
        }
        log.debug("Cupo reservado en seccion {}", seccionId);
    }

    @Transactional
    public void liberarCupo(Long seccionId) {
        if (seccionRepository.liberarCupo(seccionId) == 0) {
            log.warn("La seccion {} no tenia cupos ocupados que liberar", seccionId);
        }
    }

    /* --------------------------------- reglas --------------------------------- */

    private void validarHorario(HorarioAula horario, List<HorarioAula> anterioresDeLaSeccion, Seccion seccion,
                                Long excluirSeccionId) {
        if (!horario.getHoraInicio().isBefore(horario.getHoraFin())) {
            throw new ReglaNegocioException("La hora de inicio (" + horario.getHoraInicio()
                    + ") debe ser anterior a la hora de fin (" + horario.getHoraFin() + ")");
        }
        if (horario.getHoraInicio().isBefore(HORA_MINIMA) || horario.getHoraFin().isAfter(HORA_MAXIMA)) {
            throw new ReglaNegocioException("El horario debe estar entre " + HORA_MINIMA + " y " + HORA_MAXIMA);
        }
        for (HorarioAula otro : anterioresDeLaSeccion) {
            if (otro.seCruzaCon(horario.getDiaSemana(), horario.getHoraInicio(), horario.getHoraFin())) {
                throw new ReglaNegocioException("Los horarios de la seccion se cruzan entre si el "
                        + horario.getDiaSemana());
            }
        }
        List<HorarioAula> aulaOcupada = seccionRepository.cruceDeAula(seccion.getPeriodoId(), excluirSeccionId,
                horario.getDiaSemana(), horario.getAula(), horario.getHoraInicio(), horario.getHoraFin(),
                EstadoSeccion.CANCELADA);
        if (!aulaOcupada.isEmpty()) {
            HorarioAula conflicto = aulaOcupada.getFirst();
            throw new ReglaNegocioException("El aula " + horario.getAula() + " ya esta ocupada el "
                    + horario.getDiaSemana() + " de " + conflicto.getHoraInicio() + " a " + conflicto.getHoraFin()
                    + " por la seccion " + conflicto.getSeccion().getId() + " ("
                    + conflicto.getSeccion().getCurso().getCodigo() + ")");
        }
        if (seccion.getDocenteId() != null) {
            validarCruceDocente(seccion.getPeriodoId(), excluirSeccionId, seccion.getDocenteId(), horario);
        }
    }

    private void validarCruceDocente(Long periodoId, Long excluirSeccionId, Long docenteId, HorarioAula horario) {
        List<HorarioAula> cruces = seccionRepository.cruceDeDocente(periodoId, excluirSeccionId, docenteId,
                horario.getDiaSemana(), horario.getHoraInicio(), horario.getHoraFin(), EstadoSeccion.CANCELADA);
        if (!cruces.isEmpty()) {
            HorarioAula conflicto = cruces.getFirst();
            throw new ReglaNegocioException("El docente " + docenteId + " ya dicta la seccion "
                    + conflicto.getSeccion().getId() + " (" + conflicto.getSeccion().getCurso().getCodigo()
                    + ") el " + horario.getDiaSemana() + " de " + conflicto.getHoraInicio()
                    + " a " + conflicto.getHoraFin());
        }
    }

    private void validarPeriodo(Long periodoId) {
        PeriodoAcademicoPort.PeriodoInfo periodo = validarPeriodoExiste(periodoId);
        if (periodo.estaCerrado()) {
            throw new ReglaNegocioException("El periodo " + periodo.codigo() + " esta cerrado");
        }
    }

    private PeriodoAcademicoPort.PeriodoInfo validarPeriodoExiste(Long periodoId) {
        return periodoAcademicoPort.buscarPeriodo(periodoId)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo", periodoId));
    }

    private SeccionResponse toResponse(Seccion seccion) {
        String docenteNombre = null;
        if (seccion.getDocenteId() != null) {
            docenteNombre = nombresDeDocentes().get(seccion.getDocenteId());
        }
        return mapper.toResponse(seccion, docenteNombre);
    }

    private Map<Long, String> nombresDeDocentes() {
        return personaService.listarDocentes(null, null).stream()
                .collect(Collectors.toMap(PersonaResponse::id, PersonaResponse::nombreCompleto, (a, b) -> a));
    }

    private Seccion buscarOFallar(Long id) {
        return seccionRepository.findDetalladaById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seccion", id));
    }
}
