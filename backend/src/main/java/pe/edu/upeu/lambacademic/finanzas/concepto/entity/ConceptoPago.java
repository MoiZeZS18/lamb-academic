package pe.edu.upeu.lambacademic.finanzas.concepto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Concepto de pago con su tarifa. Reemplaza las constantes MATRICULA_BASE (150.00) y
 * COSTO_POR_CREDITO (50.00) que el modulo de Finanzas original tenia fijas en el codigo.
 */
@Entity
@Table(name = "FIN_CONCEPTOS_PAGO")
@Getter
@Setter
@NoArgsConstructor
public class ConceptoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CALCULO", nullable = false, length = 20)
    private TipoCalculo tipoCalculo;

    @Column(name = "MONTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Si es true, se incluye automaticamente en el cargo de cada matricula. */
    @Column(name = "APLICA_MATRICULA", nullable = false)
    private Boolean aplicaMatricula = Boolean.FALSE;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public enum TipoCalculo {
        /** Monto fijo por cargo. */
        FIJO,
        /** Monto multiplicado por la cantidad de creditos. */
        POR_CREDITO
    }
}
