package pe.edu.upeu.lambacademic.baseuniversal.persona.dto;

/** DTO relacionado reducido (S3 2.2): lo embeben Alumno y Expediente. */
public record PersonaResumen(
        Long id,
        String numeroDocumento,
        String nombreCompleto,
        String email,
        Long idEscuela,
        String escuelaNombre
) {
}
