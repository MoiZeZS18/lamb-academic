package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.TipoArchivo;

import java.time.LocalDateTime;

public record DocumentoAdjuntoResponse(
        Long id,
        String nombre,
        String nombreArchivo,
        TipoArchivo tipoDocumento,
        String rutaArchivo,
        LocalDateTime fechaCarga
) {
}
