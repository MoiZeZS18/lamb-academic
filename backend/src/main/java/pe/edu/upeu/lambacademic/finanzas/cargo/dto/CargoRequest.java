package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;

import java.util.List;

/**
 * Cargo manual (contrato del modulo Finanzas original, ampliado con los conceptos).
 * Si "conceptos" viene vacio se aplican los conceptos configurados para matricula.
 */
public record CargoRequest(
        @NotNull(message = "el id del estudiante es obligatorio") Long estudianteId,
        @NotNull(message = "el proceso origen es obligatorio") ProcesoOrigen procesoOrigen,
        Long referenciaId,
        @NotNull(message = "el id del periodo es obligatorio") Long periodoId,
        @Positive(message = "los creditos deben ser mayores a cero") @Max(40) Integer totalCreditos,
        @NotNull(message = "el numero de cuotas es obligatorio")
        @Min(value = 1, message = "el numero de cuotas debe ser al menos 1")
        @Max(value = 12, message = "el numero de cuotas no puede superar 12")
        Integer numeroCuotas,
        @Size(max = 500) String observacion,
        List<String> conceptos
) {
}
