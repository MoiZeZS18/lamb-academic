package pe.edu.upeu.lambacademic.finanzas.cargo.port;

import java.util.Optional;

/**
 * Puerto que Finanzas necesita para validar el periodo de un cargo manual. Lo implementa
 * Matricula (duenio del Periodo), que ya depende de Finanzas: asi no hay ciclo de modulos.
 */
public interface PeriodoFinancieroPort {

    Optional<PeriodoInfo> buscarPeriodo(Long periodoId);

    record PeriodoInfo(Long id, String codigo, String nombre) {
    }
}
