package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.EstadoExpediente;

public record CambioEstadoExpedienteRequest(
        @NotNull(message = "el estado es obligatorio") EstadoExpediente estado
) {
}
