package pe.edu.upeu.lambacademic.baseuniversal.persona.dto;

import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;

import java.time.LocalDate;

/** DTO de salida (la entidad nunca se serializa). Tambien lo reciben otros modulos. */
public record PersonaResponse(
        Long id,
        Persona.TipoPersona tipoPersona,
        Persona.TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String email,
        String telefono,
        String direccion,
        LocalDate fechaNacimiento,
        Long idEscuela,
        String escuelaNombre,
        Boolean activo
) {
}
