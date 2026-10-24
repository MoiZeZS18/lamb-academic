package pe.edu.upeu.lambacademic.baseuniversal.alumno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record AlumnoRequest(
        @NotNull(message = "la persona es obligatoria")
        Long personaId,

        @NotBlank(message = "el codigo de estudiante es obligatorio")
        @Pattern(regexp = "[A-Z0-9-]{4,20}", message = "el codigo debe tener de 4 a 20 caracteres (A-Z, 0-9, -)")
        String codigoEstudiante,

        @NotNull(message = "la fecha de ingreso es obligatoria")
        @PastOrPresent(message = "la fecha de ingreso no puede ser futura")
        LocalDate fechaIngreso
) {
}
