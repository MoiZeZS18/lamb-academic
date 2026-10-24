package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.DiaSemana;

import java.time.LocalTime;

public record HorarioAulaResponse(
        Long id,
        DiaSemana diaSemana,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
        String aula
) {
}
