package pe.edu.upeu.lambacademic.baseuniversal.alumno.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.EstadoAlumno;

public record CambioEstadoAlumnoRequest(
        @NotNull(message = "el estado es obligatorio") EstadoAlumno estado
) {
}
