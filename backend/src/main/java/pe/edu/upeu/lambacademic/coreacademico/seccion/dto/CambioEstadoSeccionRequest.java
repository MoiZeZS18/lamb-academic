package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;

public record CambioEstadoSeccionRequest(
        @NotNull(message = "el estado es obligatorio") EstadoSeccion estado
) {
}
