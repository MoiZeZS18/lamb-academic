package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import java.math.BigDecimal;

/** Contrato publico: resumen del cargo que Finanzas devuelve a Matricula. */
public record CargoGenerado(
        Long cargoId,
        String numeroCargo,
        BigDecimal montoTotal,
        Integer numeroCuotas,
        String estado,
        boolean nuevo
) {
}
