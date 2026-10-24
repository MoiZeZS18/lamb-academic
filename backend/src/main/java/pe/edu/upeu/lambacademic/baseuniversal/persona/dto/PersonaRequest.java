package pe.edu.upeu.lambacademic.baseuniversal.persona.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;

import java.time.LocalDate;

/** DTO de entrada de Persona; la validacion de forma vive aqui (@Valid en el controller). */
public record PersonaRequest(
        @NotNull(message = "el tipo de persona es obligatorio")
        Persona.TipoPersona tipoPersona,

        @NotNull(message = "el tipo de documento es obligatorio")
        Persona.TipoDocumento tipoDocumento,

        @NotBlank(message = "el numero de documento es obligatorio")
        @Pattern(regexp = "\\d{8,12}", message = "el documento debe tener entre 8 y 12 digitos")
        String numeroDocumento,

        @NotBlank(message = "los nombres son obligatorios") @Size(max = 120) String nombres,
        @NotBlank(message = "el apellido paterno es obligatorio") @Size(max = 80) String apellidoPaterno,
        @Size(max = 80) String apellidoMaterno,

        @Email(message = "el correo no es valido") @Size(max = 150)
        String email,

        @Pattern(regexp = "^$|[0-9+ ]{6,20}", message = "el telefono no es valido")
        String telefono,

        @Size(max = 255) String direccion,
        @Past(message = "la fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
        Long idEscuela
) {
}
