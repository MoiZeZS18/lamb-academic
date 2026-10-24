package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.TipoArchivo;

public record DocumentoAdjuntoRequest(
        @NotBlank(message = "el nombre del documento es obligatorio") @Size(max = 150) String nombre,
        @NotBlank(message = "el nombre del archivo es obligatorio") @Size(max = 255) String nombreArchivo,
        @NotNull(message = "el tipo de documento es obligatorio") TipoArchivo tipoDocumento,
        @NotBlank(message = "la ruta del archivo es obligatoria") @Size(max = 500) String rutaArchivo
) {
}
