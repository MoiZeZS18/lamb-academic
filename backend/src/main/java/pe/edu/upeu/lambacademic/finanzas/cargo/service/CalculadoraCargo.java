package pe.edu.upeu.lambacademic.finanzas.cargo.service;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculos financieros puros (sin base de datos), faciles de probar:
 * monto total a partir de los conceptos y fraccionamiento exacto en cuotas.
 */
public final class CalculadoraCargo {

    static final int ESCALA = 2;

    private CalculadoraCargo() {
    }

    /** Suma de conceptos: FIJO aporta su monto; POR_CREDITO, monto x creditos. */
    public static BigDecimal montoTotal(List<ConceptoPago> conceptos, Integer creditos) {
        BigDecimal total = BigDecimal.ZERO;
        for (ConceptoPago concepto : conceptos) {
            BigDecimal aporte = switch (concepto.getTipoCalculo()) {
                case FIJO -> concepto.getMonto();
                case POR_CREDITO -> {
                    if (creditos == null || creditos <= 0) {
                        throw new ReglaNegocioException("El concepto " + concepto.getCodigo()
                                + " se cobra por credito y el cargo no indica creditos");
                    }
                    yield concepto.getMonto().multiply(BigDecimal.valueOf(creditos));
                }
            };
            total = total.add(aporte);
        }
        return total.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Divide el total en n cuotas de 2 decimales. Las primeras n-1 cuotas llevan el monto
     * redondeado hacia abajo y la ultima absorbe la diferencia, de modo que la suma de las
     * cuotas es exactamente el total (el modulo original perdia o sumaba centimos).
     */
    public static List<BigDecimal> fraccionar(BigDecimal total, int numeroCuotas) {
        if (numeroCuotas < 1) {
            throw new ReglaNegocioException("El numero de cuotas debe ser al menos 1");
        }
        BigDecimal totalEscalado = total.setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal base = totalEscalado.divide(BigDecimal.valueOf(numeroCuotas), ESCALA, RoundingMode.DOWN);
        List<BigDecimal> cuotas = new ArrayList<>(Collections.nCopies(numeroCuotas - 1, base));
        BigDecimal acumulado = base.multiply(BigDecimal.valueOf(numeroCuotas - 1L));
        cuotas.add(totalEscalado.subtract(acumulado));
        return cuotas;
    }
}
