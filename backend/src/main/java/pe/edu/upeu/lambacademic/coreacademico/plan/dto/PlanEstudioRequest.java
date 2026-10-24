package pe.edu.upeu.lambacademic.coreacademico.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.DetallePlanEstudio;

import java.util.List;

/** DTO compuesto: cabecera + detalles de la malla en un unico POST atomico. */
public record PlanEstudioRequest(
        @NotNull(message = "la escuela es obligatoria") Long idEscuela,
        @NotBlank @Size(max = 25) String codigo,
        @NotBlank @Size(max = 150) String nombre,
        @NotNull @Min(2000) @Max(2100) Integer anio,
        @NotEmpty(message = "el plan debe tener al menos un curso")
        @Valid List<DetallePlanRequest> detalles
) {

    public record DetallePlanRequest(
            @NotNull(message = "el curso es obligatorio") Long idCurso,
            @NotNull @Min(1) @Max(14) Integer ciclo,
            @NotNull @Positive @Max(10) Integer creditos,
            DetallePlanEstudio.Tipo tipo,
            Long idPrerrequisito
    ) {
    }
}
