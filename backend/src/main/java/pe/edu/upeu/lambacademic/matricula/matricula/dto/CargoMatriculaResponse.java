package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import java.math.BigDecimal;

/** Cargo financiero generado por la matricula (integracion con Finanzas). */
public record CargoMatriculaResponse(
        Long cargoId,
        String numeroCargo,
        BigDecimal montoTotal,
        Integer numeroCuotas,
        String estado
) {
}
