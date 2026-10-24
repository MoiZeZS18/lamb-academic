package pe.edu.upeu.lambacademic.baseuniversal.alumno.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.AlumnoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.EstudianteResumen;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.Alumno;
import pe.edu.upeu.lambacademic.baseuniversal.persona.mapper.PersonaMapper;

@Mapper(componentModel = "spring", uses = PersonaMapper.class)
public interface AlumnoMapper {

    @Mapping(target = "habilitado", expression = "java(alumno.estaHabilitado())")
    AlumnoResponse toResponse(Alumno alumno);

    @Mapping(target = "estudianteId", source = "id")
    @Mapping(target = "personaId", source = "persona.id")
    @Mapping(target = "nombreCompleto", expression = "java(alumno.getPersona().nombreCompleto())")
    @Mapping(target = "numeroDocumento", source = "persona.numeroDocumento")
    @Mapping(target = "escuelaId", source = "persona.escuela.id")
    @Mapping(target = "habilitado", expression = "java(alumno.estaHabilitado())")
    EstudianteResumen toEstudianteResumen(Alumno alumno);
}
