package pe.edu.upeu.lambacademic.baseuniversal.institucional.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FacultadRequest(
        @NotNull(message = "la sede es obligatoria") Long idSede,
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 150) String nombre
) {
}
