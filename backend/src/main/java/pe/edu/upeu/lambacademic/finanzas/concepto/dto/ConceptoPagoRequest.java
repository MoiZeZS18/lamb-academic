package pe.edu.upeu.lambacademic.finanzas.concepto.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;

import java.math.BigDecimal;

public record ConceptoPagoRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9-]{2,20}", message = "codigo de 2 a 20 caracteres (A-Z, 0-9, -)")
        String codigo,
        @NotBlank @Size(max = 150) String nombre,
        @NotNull(message = "el tipo de calculo es obligatorio") ConceptoPago.TipoCalculo tipoCalculo,
        @NotNull @DecimalMin(value = "0.00") @DecimalMax(value = "99999.99")
        @Digits(integer = 5, fraction = 2, message = "el monto admite como maximo 2 decimales")
        BigDecimal monto,
        Boolean aplicaMatricula
) {
}
