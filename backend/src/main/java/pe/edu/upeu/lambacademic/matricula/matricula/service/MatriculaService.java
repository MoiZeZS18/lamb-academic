package pe.edu.upeu.lambacademic.matricula.matricula.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.CargoMatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaReporteResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResumenResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;

import pe.edu.upeu.lambacademic.matricula.matricula.entity.DetalleMatricula;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.Matricula;

import pe.edu.upeu.lambacademic.matricula.matricula.exception.CruceHorarioException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CupoInsuficienteException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CursoDuplicadoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.EstudianteNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.EstudianteNoHabilitadoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.MatriculaDuplicadaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.MatriculaNoEncontradaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoActivoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PrerrequisitoNoCumplidoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionDuplicadaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionNoDisponibleException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionNoEncontradaException;

import pe.edu.upeu.lambacademic.matricula.matricula.mapper.MatriculaMapper;
import pe.edu.upeu.lambacademic.matricula.matricula.port.CargoFinancieroPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.EstudianteConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.PrerrequisitoConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.SeccionConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.repository.MatriculaRepository;

import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Registro transaccional de matricula (cabecera-detalle).
 *
 * Integracion: las reglas originales (periodo activo, estudiante, seccion repetida, cupos,
 * cruce de horarios) se conservan en el mismo orden. Se agregan, marcadas con [integracion]:
 * periodo/fecha, matricula activa duplicada, seccion del periodo y abierta, curso repetido,
 * prerrequisitos, reserva atomica de cupos y generacion del cargo financiero, todo dentro
 * de la MISMA transaccion: si cualquier paso falla, no queda nada registrado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatriculaService {

    static final String ESTADO_ANULADA = "ANULADA";

    private static final Set<String> CAMPOS_ORDENABLES = Set.of("fecha", "id", "estado", "estudianteId", "periodoId");

    private final MatriculaRepository matriculaRepository;
    private final MatriculaMapper matriculaMapper;
    private final PeriodoRepository periodoRepository;
    private final SeccionConsultaPort seccionConsultaPort;
    private final EstudianteConsultaPort estudianteConsultaPort;
    private final PrerrequisitoConsultaPort prerrequisitoConsultaPort;
    private final CargoFinancieroPort cargoFinancieroPort;

    @Value("${lamb.matricula.validar-prerrequisitos:true}")
    private boolean validarPrerrequisitos = true;

    @Value("${lamb.matricula.cuotas-por-defecto:5}")
    private int cuotasPorDefecto = 5;

    @Transactional(readOnly = true)
    public List<MatriculaResumenResponse> buscarResumen() {
        return matriculaRepository.buscarResumen();
    }

    @Transactional(readOnly = true)
    public MatriculaReporteResponse generarReporte(
            Long estudianteId,
            Long periodoId,
            String estado) {

        return matriculaRepository.generarReporte(
                estudianteId,
                periodoId,
                estado
        );
    }

    /** [integracion] Antes esta consulta la hacia el controller directo contra el repositorio. */
    @Transactional(readOnly = true)
    public Page<MatriculaResponse> buscar(Long estudianteId, Long periodoId, String estado,
                                          int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "fecha";
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(sortDirection, campo));
        return matriculaRepository.buscar(estudianteId, periodoId, estado, pageable)
                .map(matriculaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MatriculaResponse obtener(Long id) {
        return matriculaMapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public MatriculaResponse registrar(
            MatriculaRequest request,
            List<DetalleMatriculaRequest> detallesRequest) {

        Periodo periodo = periodoRepository.findById(request.periodoId())
                .orElseThrow(() -> new PeriodoNoEncontradoException(
                        "El periodo " + request.periodoId() + " no existe"
                ));

        if (!"ACTIVO".equalsIgnoreCase(periodo.getEstado())) {
            throw new PeriodoNoActivoException(
                    "El periodo " + periodo.getId() +
                            " no está activo para matrícula"
            );
        }

        // [integracion] la fecha de la matricula debe caer dentro del periodo
        if (request.fecha().isBefore(periodo.getFechaInicio()) || request.fecha().isAfter(periodo.getFechaFin())) {
            throw new ReglaNegocioException("La fecha " + request.fecha() + " esta fuera del periodo "
                    + periodo.getCodigo() + " (" + periodo.getFechaInicio() + " a " + periodo.getFechaFin() + ")");
        }
        if (ESTADO_ANULADA.equalsIgnoreCase(request.estado())) {
            throw new ReglaNegocioException("Una matricula nueva no puede registrarse como ANULADA");
        }

        log.debug("Validando matricula: periodo={} estudiante={} secciones={}", periodo.getId(),
                request.estudianteId(), detallesRequest.size());

        EstudianteConsultaPort.EstudianteInfo estudiante =
                estudianteConsultaPort.consultarEstudiante(
                        request.estudianteId()
                );

        if (estudiante == null || !estudiante.existe()) {
            throw new EstudianteNoEncontradoException(
                    "El estudiante " + request.estudianteId() + " no existe"
            );
        }

        if (!estudiante.habilitado()) {
            throw new EstudianteNoHabilitadoException(
                    "El estudiante " + request.estudianteId()
                            + " no está habilitado para matrícula"
            );
        }

        // [integracion] una sola matricula activa por estudiante y periodo
        String claveActiva = claveActiva(request.estudianteId(), periodo.getId());
        if (matriculaRepository.existsByClaveActiva(claveActiva)) {
            throw new MatriculaDuplicadaException("El estudiante " + request.estudianteId()
                    + " ya tiene una matricula activa en el periodo " + periodo.getCodigo());
        }

        Set<Long> seccionesRegistradas = new HashSet<>();
        Map<Long, SeccionConsultaPort.SeccionInfo> seccionesValidadas = new HashMap<>();
        Set<Long> cursosRegistrados = new HashSet<>();

        for (DetalleMatriculaRequest detalleRequest : detallesRequest) {

            Long seccionId = detalleRequest.seccionId();

            if (!seccionesRegistradas.add(seccionId)) {
                throw new SeccionDuplicadaException(
                        "La sección " + seccionId +
                                " está repetida en la matrícula"
                );
            }

            SeccionConsultaPort.SeccionInfo seccion =
                    seccionConsultaPort.consultarSeccion(seccionId);

            if (seccion == null) {
                throw new SeccionNoEncontradaException(
                        "La sección " + seccionId + " no existe"
                );
            }

            // [integracion] la seccion debe pertenecer al periodo y estar abierta
            if (!periodo.getId().equals(seccion.periodoId())) {
                throw new SeccionNoDisponibleException("La sección " + seccionId
                        + " no pertenece al periodo " + periodo.getCodigo());
            }
            if (!seccion.abierta()) {
                throw new SeccionNoDisponibleException("La sección " + seccionId + " no está abierta para matrícula");
            }

            if (seccion.cupoDisponible() <= 0) {
                throw new CupoInsuficienteException(
                        "La sección " + seccionId +
                                " no tiene cupos disponibles"
                );
            }

            // [integracion] no se puede llevar el mismo curso en dos secciones
            if (!cursosRegistrados.add(seccion.cursoId())) {
                throw new CursoDuplicadoException("El curso " + seccion.cursoCodigo()
                        + " está repetido en la matrícula");
            }

            seccionesValidadas.put(seccionId, seccion);
        }

        // [integracion] prerrequisitos segun la malla vigente de la escuela del estudiante
        if (validarPrerrequisitos) {
            validarPrerrequisitos(request.estudianteId(), estudiante.escuelaId(), periodo,
                    seccionesValidadas.values(), cursosRegistrados);
        }

        List<SeccionConsultaPort.HorarioInfo> horarios =
                new ArrayList<>();

        for (DetalleMatriculaRequest detalleRequest : detallesRequest) {

            Long seccionId = detalleRequest.seccionId();

            List<SeccionConsultaPort.HorarioInfo> horariosSeccion =
                    seccionConsultaPort.consultarHorarios(seccionId);

            if (horariosSeccion != null) {
                horarios.addAll(horariosSeccion);
            }
        }

        for (int i = 0; i < horarios.size(); i++) {

            for (int j = i + 1; j < horarios.size(); j++) {

                SeccionConsultaPort.HorarioInfo horario1 =
                        horarios.get(i);

                SeccionConsultaPort.HorarioInfo horario2 =
                        horarios.get(j);

                if (existeCruce(horario1, horario2)) {
                    throw new CruceHorarioException(
                            "Existe un cruce de horarios entre las secciones"
                    );
                }
            }
        }

        Matricula matricula = matriculaMapper.toEntity(request);
        matricula.setClaveActiva(claveActiva);

        int totalCreditos = 0;
        for (DetalleMatriculaRequest detalleRequest : detallesRequest) {

            Long seccionId = detalleRequest.seccionId();
            SeccionConsultaPort.SeccionInfo seccion = seccionesValidadas.get(seccionId);

            DetalleMatricula detalle = new DetalleMatricula();

            detalle.setSeccionId(seccionId);
            detalle.setCursoId(seccion.cursoId());
            detalle.setCreditos(seccion.creditos());
            detalle.setMatricula(matricula);

            matricula.getDetalles().add(detalle);
            totalCreditos += seccion.creditos() == null ? 0 : seccion.creditos();
        }
        matricula.setTotalCreditos(totalCreditos);

        try {
            matriculaRepository.saveAndFlush(matricula);
        } catch (DataIntegrityViolationException ex) {
            // Dos solicitudes simultaneas del mismo estudiante: UK_MATRICULA_ACTIVA rechaza la segunda.
            throw new MatriculaDuplicadaException("El estudiante " + request.estudianteId()
                    + " ya tiene una matricula activa en el periodo " + periodo.getCodigo());
        }

        // [integracion] reserva atomica de cupos: si una seccion se llena entre la validacion
        // y este punto, la excepcion revierte toda la matricula (y los cupos ya reservados).
        // Orden ascendente de id: dos matriculas concurrentes bloquean las filas en el mismo
        // orden y no pueden interbloquearse (ORA-00060).
        for (Long seccionId : new TreeSet<>(seccionesRegistradas)) {
            seccionConsultaPort.reservarCupo(seccionId);
        }

        // [integracion] cargo financiero en la misma transaccion (idempotente por matricula)
        int cuotas = request.numeroCuotas() == null ? cuotasPorDefecto : request.numeroCuotas();
        CargoFinancieroPort.CargoInfo cargo = cargoFinancieroPort.generarCargo(matricula.getId(),
                matricula.getEstudianteId(), periodo.getId(), periodo.getCodigo(), totalCreditos, cuotas);

        log.info("Matricula registrada id={} estudiante={} periodo={} secciones={} creditos={} cargo={}",
                matricula.getId(), matricula.getEstudianteId(), periodo.getCodigo(), seccionesRegistradas.size(),
                totalCreditos, cargo.numeroCargo());

        return matriculaMapper.toResponse(matricula).conCargo(toCargoResponse(cargo));
    }

    /**
     * [integracion] Anula la matricula: libera los cupos, anula el cargo (si no tiene pagos) y
     * deja libre la clave activa para que el estudiante pueda volver a matricularse.
     */
    @Transactional
    public MatriculaResponse anular(Long id) {
        Matricula matricula = buscarOFallar(id);
        if (ESTADO_ANULADA.equalsIgnoreCase(matricula.getEstado())) {
            throw new ReglaNegocioException("La matricula " + id + " ya esta anulada");
        }
        cargoFinancieroPort.anularCargo(id);
        if (matricula.getClaveActiva() != null) {
            // Solo las matriculas registradas con la integracion reservaron cupos reales.
            matricula.getDetalles().stream().map(DetalleMatricula::getSeccionId).sorted()
                    .forEach(seccionConsultaPort::liberarCupo);
        }
        matricula.setEstado(ESTADO_ANULADA);
        matricula.setClaveActiva(null);
        matricula.setFechaAnulacion(LocalDateTime.now());
        Matricula guardada = matriculaRepository.save(matricula);
        log.info("Matricula anulada id={} estudiante={}", id, matricula.getEstudianteId());
        return matriculaMapper.toResponse(guardada);
    }

    /**
     * [integracion] Recuperacion: genera el cargo de una matricula que no lo tenga (por ejemplo,
     * las registradas antes de integrar Finanzas). Idempotente: no duplica cargos.
     */
    @Transactional
    public MatriculaResponse generarCargo(Long id, Integer numeroCuotas) {
        Matricula matricula = buscarOFallar(id);
        if (ESTADO_ANULADA.equalsIgnoreCase(matricula.getEstado())) {
            throw new ReglaNegocioException("La matricula " + id + " esta anulada");
        }
        if (matricula.getTotalCreditos() == null || matricula.getTotalCreditos() <= 0) {
            throw new ReglaNegocioException("La matricula " + id
                    + " no registra creditos (anterior a la integracion); no se puede calcular su cargo");
        }
        Periodo periodo = periodoRepository.findById(matricula.getPeriodoId())
                .orElseThrow(() -> new PeriodoNoEncontradoException("El periodo " + matricula.getPeriodoId() + " no existe"));
        int cuotas = numeroCuotas == null ? cuotasPorDefecto : numeroCuotas;
        CargoFinancieroPort.CargoInfo cargo = cargoFinancieroPort.generarCargo(matricula.getId(),
                matricula.getEstudianteId(), periodo.getId(), periodo.getCodigo(), matricula.getTotalCreditos(), cuotas);
        return matriculaMapper.toResponse(matricula).conCargo(toCargoResponse(cargo));
    }

    private void validarPrerrequisitos(Long estudianteId, Long escuelaId, Periodo periodo,
                                       Iterable<SeccionConsultaPort.SeccionInfo> secciones,
                                       Set<Long> cursosDeEstaMatricula) {
        for (SeccionConsultaPort.SeccionInfo seccion : secciones) {
            List<PrerrequisitoConsultaPort.PrerrequisitoInfo> prerrequisitos =
                    prerrequisitoConsultaPort.prerrequisitosDe(seccion.cursoId(), escuelaId);
            if (prerrequisitos.isEmpty()) {
                continue;
            }
            List<Long> ids = prerrequisitos.stream().map(PrerrequisitoConsultaPort.PrerrequisitoInfo::cursoId).toList();
            Set<Long> llevados = new HashSet<>(
                    matriculaRepository.cursosLlevadosAntesDe(estudianteId, ids, periodo.getFechaInicio()));
            for (PrerrequisitoConsultaPort.PrerrequisitoInfo prerrequisito : prerrequisitos) {
                if (!llevados.contains(prerrequisito.cursoId())) {
                    String detalle = cursosDeEstaMatricula.contains(prerrequisito.cursoId())
                            ? " (no puede llevarse en el mismo periodo)" : "";
                    throw new PrerrequisitoNoCumplidoException("Para matricularse en " + seccion.cursoCodigo()
                            + " debe haber llevado antes " + prerrequisito.codigo() + detalle);
                }
            }
        }
    }

    private boolean existeCruce(
            SeccionConsultaPort.HorarioInfo horario1,
            SeccionConsultaPort.HorarioInfo horario2) {

        if (!horario1.dia().equalsIgnoreCase(horario2.dia())) {
            return false;
        }

        LocalTime inicio1 = horario1.horaInicio();
        LocalTime fin1 = horario1.horaFin();

        LocalTime inicio2 = horario2.horaInicio();
        LocalTime fin2 = horario2.horaFin();

        return inicio1.isBefore(fin2)
                && inicio2.isBefore(fin1);
    }

    private Matricula buscarOFallar(Long id) {
        return matriculaRepository.findDetalladaById(id)
                .orElseThrow(() -> new MatriculaNoEncontradaException("La matricula " + id + " no existe"));
    }

    static String claveActiva(Long estudianteId, Long periodoId) {
        return estudianteId + "-" + periodoId;
    }

    private static CargoMatriculaResponse toCargoResponse(CargoFinancieroPort.CargoInfo cargo) {
        return new CargoMatriculaResponse(cargo.cargoId(), cargo.numeroCargo(), cargo.montoTotal(),
                cargo.numeroCuotas(), cargo.estado());
    }
}
