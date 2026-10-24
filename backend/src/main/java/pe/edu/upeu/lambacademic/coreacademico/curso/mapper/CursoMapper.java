package pe.edu.upeu.lambacademic.coreacademico.curso.mapper;

import org.mapstruct.Mapper;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResponse;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResumen;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;

@Mapper(componentModel = "spring")
public interface CursoMapper {

    CursoResponse toResponse(Curso curso);

    CursoResumen toResumen(Curso curso);
}
