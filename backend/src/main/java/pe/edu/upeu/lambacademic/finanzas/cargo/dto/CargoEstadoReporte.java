package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;

import java.math.BigDecimal;

/** Fila del reporte agregado de cargos por estado. */
public record CargoEstadoReporte(EstadoCargo estado, Long cantidad, BigDecimal montoTotal, BigDecimal saldoPendiente) {
}
