package pe.edu.upeu.lambacademic.coreacademico.seccion.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.coreacademico.curso.mapper.CursoMapper;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.HorarioAulaRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.HorarioAulaResponse;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionMatriculaInfo;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionResponse;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.HorarioAula;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.Seccion;

@Mapper(componentModel = "spring", uses = CursoMapper.class)
public interface SeccionMapper {

    @Mapping(target = "vacantesDisponibles", expression = "java(seccion.vacantesDisponibles())")
    @Mapping(target = "docenteNombre", source = "docenteNombre")
    SeccionResponse toResponse(Seccion seccion, String docenteNombre);

    HorarioAulaResponse toResponse(HorarioAula horario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seccion", ignore = true)
    @Mapping(target = "aula", expression = "java(request.aula().trim().toUpperCase())")
    HorarioAula toEntity(HorarioAulaRequest request);

    @Mapping(target = "seccionId", source = "id")
    @Mapping(target = "cursoId", source = "curso.id")
    @Mapping(target = "cursoCodigo", source = "curso.codigo")
    @Mapping(target = "cursoNombre", source = "curso.nombre")
    @Mapping(target = "creditos", source = "curso.creditos")
    SeccionMatriculaInfo toMatriculaInfo(Seccion seccion);

    @Mapping(target = "dia", source = "diaSemana")
    SeccionMatriculaInfo.Horario toHorarioInfo(HorarioAula horario);
}
