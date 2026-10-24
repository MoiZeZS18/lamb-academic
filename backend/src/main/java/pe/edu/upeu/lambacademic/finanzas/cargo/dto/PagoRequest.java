package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.MedioPago;

import java.math.BigDecimal;

public record PagoRequest(
        @NotNull(message = "el monto es obligatorio")
        @DecimalMin(value = "0.01", message = "el monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "el monto admite como maximo 2 decimales")
        BigDecimal monto,
        @NotNull(message = "el medio de pago es obligatorio") MedioPago medioPago,
        @Size(max = 100) String referencia
) {
}
