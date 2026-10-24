package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

import pe.edu.upeu.lambacademic.finanzas.cargo.entity.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(Long id, Long cargoId, BigDecimal monto, LocalDateTime fechaPago, MedioPago medioPago,
                           String referencia) {
}
