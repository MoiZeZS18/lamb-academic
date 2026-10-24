package pe.edu.upeu.lambacademic.baseuniversal.institucional.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.FacultadResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.SedeResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Escuela;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Facultad;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Sede;

@Mapper(componentModel = "spring")
public interface InstitucionalMapper {

    SedeResponse toResponse(Sede sede);

    @Mapping(target = "idSede", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    FacultadResponse toResponse(Facultad facultad);

    @Mapping(target = "idFacultad", source = "facultad.id")
    @Mapping(target = "facultadNombre", source = "facultad.nombre")
    EscuelaResponse toResponse(Escuela escuela);
}
