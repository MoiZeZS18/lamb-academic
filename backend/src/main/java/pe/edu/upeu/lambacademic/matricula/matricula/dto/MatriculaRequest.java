package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MatriculaRequest(

        @NotNull
        Long estudianteId,

        @NotNull
        Long periodoId,

        @NotNull
        LocalDate fecha,

        @NotBlank
        @Size(max = 20)
        String estado,

        /** Opcional (integracion con Finanzas): cuotas del cargo; si falta, se usa la configurada. */
        @Min(value = 1, message = "el numero de cuotas debe ser al menos 1")
        @Max(value = 12, message = "el numero de cuotas no puede superar 12")
        Integer numeroCuotas
) {
}