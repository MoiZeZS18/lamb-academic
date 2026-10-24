package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import java.math.BigDecimal;

/** Saldo consolidado (contrato del modulo Finanzas original + cuotas vencidas). */
public record SaldoEstudianteResponse(
        Long estudianteId,
        BigDecimal deudaTotal,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        Integer totalCargosPendientes,
        Long cuotasVencidas
) {
}
