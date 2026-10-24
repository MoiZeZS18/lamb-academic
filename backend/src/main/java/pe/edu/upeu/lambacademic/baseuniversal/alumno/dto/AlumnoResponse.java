package pe.edu.upeu.lambacademic.baseuniversal.alumno.dto;

import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.EstadoAlumno;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResumen;

import java.time.LocalDate;

public record AlumnoResponse(
        Long id,
        String codigoEstudiante,
        EstadoAlumno estado,
        LocalDate fechaIngreso,
        boolean habilitado,
        PersonaResumen persona
) {
}
