package pe.edu.upeu.lambacademic.coreacademico.curso.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CursoRequest(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 150) String nombre,
        @NotNull @Min(1) @Max(10) Integer creditos,
        @PositiveOrZero @Max(20) Integer horasTeoricas,
        @PositiveOrZero @Max(20) Integer horasPracticas
) {
}
