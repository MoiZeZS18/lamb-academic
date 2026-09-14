package pe.edu.upeu.lambacademic.matricula.matricula.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.Matricula;

@Mapper(componentModel = "spring", uses = DetalleMatriculaMapper.class)
public interface MatriculaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    Matricula toEntity(MatriculaRequest request);

    MatriculaResponse toResponse(Matricula matricula);
}