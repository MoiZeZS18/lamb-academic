package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MatriculaRequest(

        @NotNull
        Long estudianteId,

        @NotNull
        Long periodoId,

        @NotNull
        LocalDate fecha,

        @NotBlank
        @Size(max = 20)
        String estado
) {
}