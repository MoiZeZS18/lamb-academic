package pe.edu.upeu.lambacademic.baseuniversal.expediente.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.DocumentoAdjuntoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.DocumentoAdjuntoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteResponse;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.DocumentoAdjunto;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.Expediente;
import pe.edu.upeu.lambacademic.baseuniversal.persona.mapper.PersonaMapper;

@Mapper(componentModel = "spring", uses = PersonaMapper.class)
public interface ExpedienteMapper {

    ExpedienteResponse toResponse(Expediente expediente);

    DocumentoAdjuntoResponse toResponse(DocumentoAdjunto documento);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expediente", ignore = true)
    @Mapping(target = "fechaCarga", ignore = true)
    DocumentoAdjunto toEntity(DocumentoAdjuntoRequest request);
}
