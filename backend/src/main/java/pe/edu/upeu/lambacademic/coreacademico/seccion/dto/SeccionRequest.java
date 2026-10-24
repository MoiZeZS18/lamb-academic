package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTO compuesto: seccion + horarios en un solo POST (programacion atomica). */
public record SeccionRequest(
        @NotNull(message = "el curso es obligatorio") Long cursoId,
        @NotNull(message = "el periodo es obligatorio") Long periodoId,
        @NotNull(message = "el grupo es obligatorio")
        @Pattern(regexp = "[A-Z0-9]{1,10}", message = "el grupo debe tener de 1 a 10 caracteres (A-Z, 0-9)")
        String grupo,
        Long docenteId,
        @NotNull(message = "el limite de vacantes es obligatorio")
        @Min(value = 1, message = "el limite de vacantes debe ser al menos 1")
        @Max(value = 200, message = "el limite de vacantes no puede superar 200")
        Integer limiteVacantes,
        @NotEmpty(message = "la seccion debe tener al menos un horario")
        @Size(max = 10, message = "una seccion admite como maximo 10 bloques horarios")
        @Valid List<HorarioAulaRequest> horarios
) {
}
