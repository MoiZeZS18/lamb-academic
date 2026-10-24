package pe.edu.upeu.lambacademic.finanzas.cargo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DETALLE del cargo: una cuota del fraccionamiento. */
@Entity
@Table(name = "FIN_DETALLE_CARGOS")
@Getter
@Setter
@NoArgsConstructor
public class DetalleCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_CARGO", nullable = false)
    private Cargo cargo;

    @Column(name = "NUMERO_CUOTA", nullable = false)
    private Integer numeroCuota;

    @Column(name = "CONCEPTO_NOMBRE", nullable = false, length = 150)
    private String conceptoNombre;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "MONTO_CUOTA", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCuota;

    @Column(name = "MONTO_PAGADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoCuota estado = EstadoCuota.PENDIENTE;

    public BigDecimal saldo() {
        return montoCuota.subtract(montoPagado);
    }
}
