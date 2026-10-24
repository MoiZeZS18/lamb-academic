package pe.edu.upeu.lambacademic.baseuniversal.persona.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResumen;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;

@Mapper(componentModel = "spring")
public interface PersonaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "escuela", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Persona toEntity(PersonaRequest request);

    @Mapping(target = "nombreCompleto", expression = "java(persona.nombreCompleto())")
    @Mapping(target = "idEscuela", source = "escuela.id")
    @Mapping(target = "escuelaNombre", source = "escuela.nombre")
    PersonaResponse toResponse(Persona persona);

    @Mapping(target = "nombreCompleto", expression = "java(persona.nombreCompleto())")
    @Mapping(target = "idEscuela", source = "escuela.id")
    @Mapping(target = "escuelaNombre", source = "escuela.nombre")
    PersonaResumen toResumen(Persona persona);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "escuela", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void actualizar(@MappingTarget Persona persona, PersonaRequest request);
}
