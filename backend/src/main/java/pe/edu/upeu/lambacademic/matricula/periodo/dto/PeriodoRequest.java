package pe.edu.upeu.lambacademic.matricula.periodo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PeriodoRequest(

        @NotBlank
        @Size(max = 20)
        String codigo,

        @NotBlank
        @Size(max = 100)
        String nombre,

        @NotNull
        LocalDate fechaInicio,

        @NotNull
        LocalDate fechaFin,

        @NotBlank
        @Pattern(regexp = "PLANIFICADO|ACTIVO|CERRADO", message = "el estado debe ser PLANIFICADO, ACTIVO o CERRADO")
        String estado
) {
}