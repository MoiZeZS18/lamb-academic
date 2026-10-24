package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.EstadoExpediente;

import java.time.LocalDateTime;

/** Proyeccion para listados: cabecera sin la coleccion de documentos. */
public record ExpedienteResumen(
        Long id,
        String codigoTramite,
        String asunto,
        LocalDateTime fechaRegistro,
        EstadoExpediente estado,
        Integer totalDocumentos,
        Long personaId,
        String personaNombre
) {
}
