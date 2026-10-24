package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTO compuesto: cabecera del expediente + coleccion de documentos en un solo POST. */
public record ExpedienteRequest(
        @NotBlank(message = "el codigo de tramite es obligatorio") @Size(max = 50) String codigoTramite,
        @NotBlank(message = "el asunto es obligatorio") @Size(max = 255) String asunto,
        @NotNull(message = "la persona es obligatoria") Long personaId,
        @NotEmpty(message = "el expediente debe tener al menos un documento")
        @Size(max = 20, message = "un expediente admite como maximo 20 documentos")
        @Valid List<DocumentoAdjuntoRequest> documentos
) {
}
