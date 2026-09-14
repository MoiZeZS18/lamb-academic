package pe.edu.upeu.lambacademic.matricula.matricula.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.DetalleMatricula;

@Mapper(componentModel = "spring")
public interface DetalleMatriculaMapper {

    @Mapping(target = "seccionId", source = "seccionId")
    DetalleMatriculaResponse toResponse(DetalleMatricula detalleMatricula);
}