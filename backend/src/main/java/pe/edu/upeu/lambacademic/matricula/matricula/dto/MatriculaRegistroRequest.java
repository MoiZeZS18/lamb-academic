package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MatriculaRegistroRequest(

        @NotNull
        @Valid
        MatriculaRequest matricula,

        @NotEmpty(message = "la matricula debe tener al menos una seccion")
        @Size(max = 8, message = "una matricula admite como maximo 8 secciones")
        @Valid
        List<DetalleMatriculaRequest> detalles
) {
}