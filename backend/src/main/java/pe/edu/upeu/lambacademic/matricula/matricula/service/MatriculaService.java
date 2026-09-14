package pe.edu.upeu.lambacademic.matricula.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaReporteResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResumenResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;

import pe.edu.upeu.lambacademic.matricula.matricula.entity.DetalleMatricula;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.Matricula;

import pe.edu.upeu.lambacademic.matricula.matricula.exception.CupoInsuficienteException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CruceHorarioException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoActivoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionDuplicadaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.EstudianteNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.EstudianteNoHabilitadoException;

import pe.edu.upeu.lambacademic.matricula.matricula.mapper.MatriculaMapper;
import pe.edu.upeu.lambacademic.matricula.matricula.port.EstudianteConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.SeccionConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.repository.MatriculaRepository;

import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final MatriculaMapper matriculaMapper;
    private final PeriodoRepository periodoRepository;
    private final SeccionConsultaPort seccionConsultaPort;
    private final EstudianteConsultaPort estudianteConsultaPort;

    public List<MatriculaResumenResponse> buscarResumen() {
        return matriculaRepository.buscarResumen();
    }

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

        System.out.println(">>> VALIDANDO PERIODO: " + request.periodoId());
        System.out.println(">>> PERIODO ENCONTRADO: " + periodo.getId());

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

        Set<Long> seccionesRegistradas = new HashSet<>();

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
                throw new IllegalArgumentException(
                        "La sección " + seccionId + " no existe"
                );
            }

            if (seccion.cupoDisponible() <= 0) {
                throw new CupoInsuficienteException(
                        "La sección " + seccionId +
                                " no tiene cupos disponibles"
                );
            }
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

        matriculaRepository.saveAndFlush(matricula);

        for (DetalleMatriculaRequest detalleRequest : detallesRequest) {

            Long seccionId = detalleRequest.seccionId();

            DetalleMatricula detalle = new DetalleMatricula();

            detalle.setSeccionId(seccionId);
            detalle.setMatricula(matricula);

            matricula.getDetalles().add(detalle);

            matriculaRepository.saveAndFlush(matricula);
        }

        return matriculaMapper.toResponse(matricula);
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
}