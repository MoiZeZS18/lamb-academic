package pe.edu.upeu.lambacademic.finanzas.cargo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraCargoTest {

    private static ConceptoPago concepto(String codigo, ConceptoPago.TipoCalculo tipo, String monto) {
        ConceptoPago c = new ConceptoPago();
        c.setCodigo(codigo);
        c.setTipoCalculo(tipo);
        c.setMonto(new BigDecimal(monto));
        return c;
    }

    @Test
    @DisplayName("Monto total = conceptos fijos + conceptos por credito x creditos")
    void montoTotalCombinaConceptos() {
        List<ConceptoPago> conceptos = List.of(
                concepto("MAT-BASE", ConceptoPago.TipoCalculo.FIJO, "150.00"),
                concepto("CRED", ConceptoPago.TipoCalculo.POR_CREDITO, "50.00"));

        assertThat(CalculadoraCargo.montoTotal(conceptos, 11)).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("Un concepto por credito sin creditos es un error de negocio")
    void porCreditoSinCreditosFalla() {
        List<ConceptoPago> conceptos = List.of(concepto("CRED", ConceptoPago.TipoCalculo.POR_CREDITO, "50.00"));

        assertThatThrownBy(() -> CalculadoraCargo.montoTotal(conceptos, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("por credito");
    }

    @ParameterizedTest(name = "{0} en {1} cuotas suma exactamente el total")
    @CsvSource({"1150.00,3", "700.00,5", "100.00,3", "0.05,4", "999.99,12", "1000.00,1"})
    @DisplayName("El fraccionamiento no pierde ni agrega centimos")
    void fraccionarSumaExacta(String total, int cuotas) {
        List<BigDecimal> montos = CalculadoraCargo.fraccionar(new BigDecimal(total), cuotas);

        assertThat(montos).hasSize(cuotas);
        assertThat(montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(total);
        assertThat(montos).allSatisfy(m -> assertThat(m.scale()).isEqualTo(2));
    }

    @Test
    @DisplayName("Con 1150 en 3 cuotas: 383.33, 383.33 y la ultima 383.34 (antes: 3 x 383.33 = 1149.99)")
    void ultimaCuotaAbsorbeDiferencia() {
        assertThat(CalculadoraCargo.fraccionar(new BigDecimal("1150.00"), 3))
                .containsExactly(new BigDecimal("383.33"), new BigDecimal("383.33"), new BigDecimal("383.34"));
    }

    @Test
    void cuotasMenoresAUnoFallan() {
        assertThatThrownBy(() -> CalculadoraCargo.fraccionar(BigDecimal.TEN, 0))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
