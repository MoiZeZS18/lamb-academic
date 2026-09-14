package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import jakarta.validation.constraints.NotNull;

public record DetalleMatriculaRequest(

        @NotNull
        Long seccionId
) {
}