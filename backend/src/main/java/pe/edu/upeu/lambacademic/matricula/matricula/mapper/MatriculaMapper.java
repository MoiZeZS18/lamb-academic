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
    @Mapping(target = "totalCreditos", ignore = true)
    @Mapping(target = "claveActiva", ignore = true)
    @Mapping(target = "fechaAnulacion", ignore = true)
    Matricula toEntity(MatriculaRequest request);

    /** El cargo lo agrega el service cuando corresponde (no forma parte de la entidad). */
    @Mapping(target = "cargo", ignore = true)
    MatriculaResponse toResponse(Matricula matricula);
}
