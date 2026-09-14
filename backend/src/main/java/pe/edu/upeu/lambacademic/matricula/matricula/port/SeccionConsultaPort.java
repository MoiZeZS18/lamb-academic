package pe.edu.upeu.lambacademic.matricula.matricula.port;

import java.time.LocalTime;
import java.util.List;

public interface SeccionConsultaPort {

    SeccionInfo consultarSeccion(Long seccionId);

    List<HorarioInfo> consultarHorarios(Long seccionId);

    record SeccionInfo(
            Long seccionId,
            Integer cupoTotal,
            Integer cupoDisponible
    ) {
    }

    record HorarioInfo(
            String dia,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
    }
}