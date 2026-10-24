package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cambios permitidos sobre una seccion ya programada (curso, periodo y grupo no cambian). */
public record SeccionActualizacionRequest(
        Long docenteId,
        @NotNull(message = "el limite de vacantes es obligatorio")
        @Min(value = 1, message = "el limite de vacantes debe ser al menos 1")
        @Max(value = 200, message = "el limite de vacantes no puede superar 200")
        Integer limiteVacantes
) {
}
