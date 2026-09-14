package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MatriculaRegistroRequest(

        @NotNull
        @Valid
        MatriculaRequest matricula,

        @NotNull
        @Valid
        List<DetalleMatriculaRequest> detalles
) {
}