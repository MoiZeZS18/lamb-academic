package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import pe.edu.upeu.lambacademic.matricula.matricula.entity.ResultadoCurso;

public record DetalleMatriculaResponse(
        Long id,
        Long seccionId,
        Long cursoId,
        Integer creditos,
        ResultadoCurso resultado
) {
}