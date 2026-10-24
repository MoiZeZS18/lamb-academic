package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.ResultadoCurso;

public record ResultadoCursoRequest(
        @NotNull(message = "el resultado es obligatorio (APROBADO, DESAPROBADO o RETIRADO)")
        ResultadoCurso resultado
) {
}
