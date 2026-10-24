package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.DiaSemana;

import java.time.LocalTime;

public record HorarioAulaRequest(
        @NotNull(message = "el dia de la semana es obligatorio") DiaSemana diaSemana,
        @NotNull(message = "la hora de inicio es obligatoria") @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @NotNull(message = "la hora de fin es obligatoria") @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
        @NotBlank(message = "el aula es obligatoria") @Size(max = 50) String aula
) {
}
