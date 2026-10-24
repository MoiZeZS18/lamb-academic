package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CargoResponse(
        Long id,
        String numeroCargo,
        Long estudianteId,
        ProcesoOrigen procesoOrigen,
        Long referenciaId,
        Long periodoId,
        Integer totalCreditos,
        Integer numeroCuotas,
        BigDecimal montoTotal,
        BigDecimal saldoPendiente,
        EstadoCargo estado,
        String observacion,
        LocalDateTime fechaEmision,
        List<CuotaResponse> cuotas
) {
}
