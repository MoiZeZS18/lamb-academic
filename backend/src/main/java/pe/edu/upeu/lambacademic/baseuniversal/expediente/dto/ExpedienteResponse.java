package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.EstadoExpediente;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResumen;

import java.time.LocalDateTime;
import java.util.List;

public record ExpedienteResponse(
        Long id,
        String codigoTramite,
        String asunto,
        LocalDateTime fechaRegistro,
        EstadoExpediente estado,
        Integer totalDocumentos,
        PersonaResumen persona,
        List<DocumentoAdjuntoResponse> documentos
) {
}
