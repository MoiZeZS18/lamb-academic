package pe.edu.upeu.lambacademic.matricula.matricula.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Generacion y anulacion del cargo financiero de una matricula (Finanzas). */
public interface CargoFinancieroPort {

    /** Idempotente: si la matricula ya tiene cargo, lo devuelve sin duplicarlo. */
    CargoInfo generarCargo(Long matriculaId, Long estudianteId, Long periodoId, String periodoCodigo,
                           Integer totalCreditos, Integer numeroCuotas);

    void anularCargo(Long matriculaId);

    /** Solo lectura: el cargo de la matricula, si existe (nunca lo genera). */
    Optional<CargoInfo> consultarCargo(Long matriculaId);

    /** Solo lectura en lote: matriculaId -> cargo (las matriculas sin cargo no aparecen). */
    Map<Long, CargoInfo> consultarCargos(Collection<Long> matriculaIds);

    record CargoInfo(Long cargoId, String numeroCargo, BigDecimal montoTotal, Integer numeroCuotas,
                     String estado) {
    }
}
