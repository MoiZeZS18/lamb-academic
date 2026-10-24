package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Proyeccion de listado: cabecera del cargo sin cuotas. */
public record CargoResumenResponse(
        Long id,
        String numeroCargo,
        Long estudianteId,
        ProcesoOrigen procesoOrigen,
        Long referenciaId,
        Long periodoId,
        BigDecimal montoTotal,
        BigDecimal saldoPendiente,
        Integer numeroCuotas,
        EstadoCargo estado,
        LocalDateTime fechaEmision
) {
}
