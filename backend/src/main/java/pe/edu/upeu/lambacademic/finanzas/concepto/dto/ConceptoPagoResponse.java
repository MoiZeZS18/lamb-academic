package pe.edu.upeu.lambacademic.finanzas.concepto.dto;

import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;

import java.math.BigDecimal;

public record ConceptoPagoResponse(
        Long id,
        String codigo,
        String nombre,
        ConceptoPago.TipoCalculo tipoCalculo,
        BigDecimal monto,
        Boolean aplicaMatricula,
        Boolean activo
) {
}
