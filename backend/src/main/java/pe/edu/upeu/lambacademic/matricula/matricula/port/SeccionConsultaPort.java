package pe.edu.upeu.lambacademic.matricula.matricula.port;

import java.time.LocalTime;
import java.util.List;

public interface SeccionConsultaPort {

    /** null si la seccion no existe. */
    SeccionInfo consultarSeccion(Long seccionId);

    List<HorarioInfo> consultarHorarios(Long seccionId);

    /** Ocupa un cupo de forma atomica; lanza una excepcion de negocio si ya no hay vacantes. */
    void reservarCupo(Long seccionId);

    void liberarCupo(Long seccionId);

    /**
     * cursoId, cursoCodigo, creditos, periodoId y abierta se agregaron en la integracion
     * con Core Academico (antes el adaptador simulado solo devolvia los cupos).
     */
    record SeccionInfo(
            Long seccionId,
            Integer cupoTotal,
            Integer cupoDisponible,
            Long cursoId,
            String cursoCodigo,
            Integer creditos,
            Long periodoId,
            boolean abierta
    ) {
    }

    record HorarioInfo(
            String dia,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
    }
}
