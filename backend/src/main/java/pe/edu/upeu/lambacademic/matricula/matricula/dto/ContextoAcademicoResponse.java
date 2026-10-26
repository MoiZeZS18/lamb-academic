package pe.edu.upeu.lambacademic.matricula.matricula.dto;

/**
 * [U2] Contexto academico del alumno para armar la inscripcion: su escuela (para resolver el plan de
 * estudios vigente y los prerrequisitos que valida registrar) y si esta habilitado para matricularse.
 * Sale de Base Universal a traves de EstudianteConsultaPort, el mismo puerto que usa el registro.
 */
public record ContextoAcademicoResponse(
        Long estudianteId,
        boolean existe,
        boolean habilitado,
        Long escuelaId
) {
}
