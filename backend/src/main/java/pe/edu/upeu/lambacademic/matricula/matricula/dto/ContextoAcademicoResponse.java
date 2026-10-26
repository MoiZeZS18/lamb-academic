package pe.edu.upeu.lambacademic.matricula.matricula.dto;

/**
 * Escuela del alumno (para resolver su plan vigente y los prerrequisitos que valida el registro) y
 * si esta habilitado para matricularse. Sale de Base Universal por EstudianteConsultaPort.
 */
public record ContextoAcademicoResponse(
        Long estudianteId,
        boolean existe,
        boolean habilitado,
        Long escuelaId
) {
}
