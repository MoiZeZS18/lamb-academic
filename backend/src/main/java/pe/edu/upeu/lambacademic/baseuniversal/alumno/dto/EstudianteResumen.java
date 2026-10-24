package pe.edu.upeu.lambacademic.baseuniversal.alumno.dto;

/**
 * Contrato publico de Base Universal para Matricula y Finanzas: lo minimo que necesitan
 * saber de un estudiante, sin exponer las entidades Alumno ni Persona.
 */
public record EstudianteResumen(
        Long estudianteId,
        Long personaId,
        String codigoEstudiante,
        String nombreCompleto,
        String numeroDocumento,
        Long escuelaId,
        String estado,
        boolean habilitado
) {
}
