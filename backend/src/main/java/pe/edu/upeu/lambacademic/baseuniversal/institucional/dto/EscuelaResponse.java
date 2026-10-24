package pe.edu.upeu.lambacademic.baseuniversal.institucional.dto;

/** DTO publico: Core Academico lo recibe para validar la escuela de un plan de estudios. */
public record EscuelaResponse(
        Long id,
        Long idFacultad,
        String facultadNombre,
        String codigo,
        String nombre,
        String nivel,
        Boolean activo
) {
}
