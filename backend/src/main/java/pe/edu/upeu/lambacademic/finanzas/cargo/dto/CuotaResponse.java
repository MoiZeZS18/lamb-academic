package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCuota;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaResponse(
        Long id,
        Integer numeroCuota,
        String conceptoNombre,
        LocalDate fechaVencimiento,
        BigDecimal montoCuota,
        BigDecimal montoPagado,
        EstadoCuota estado
) {
}
