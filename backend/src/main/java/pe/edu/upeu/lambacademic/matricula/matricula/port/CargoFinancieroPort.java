package pe.edu.upeu.lambacademic.matricula.matricula.port;

import java.math.BigDecimal;

/** Generacion y anulacion del cargo financiero de una matricula (Finanzas). */
public interface CargoFinancieroPort {

    /** Idempotente: si la matricula ya tiene cargo, lo devuelve sin duplicarlo. */
    CargoInfo generarCargo(Long matriculaId, Long estudianteId, Long periodoId, String periodoCodigo,
                           Integer totalCreditos, Integer numeroCuotas);

    void anularCargo(Long matriculaId);

    record CargoInfo(Long cargoId, String numeroCargo, BigDecimal montoTotal, Integer numeroCuotas,
                     String estado) {
    }
}
