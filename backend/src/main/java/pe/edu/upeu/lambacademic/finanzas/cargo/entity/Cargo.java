package pe.edu.upeu.lambacademic.finanzas.cargo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CABECERA del cargo al estudiante (origen: Frinanzas-lamb). Documento historico: guarda
 * ids de estudiante, periodo y referencia sin relacion JPA hacia otros modulos (S4, Tabla 2).
 */
@Entity
@Table(name = "FIN_CARGOS")
@Getter
@Setter
@NoArgsConstructor
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NUMERO_CARGO", nullable = false, unique = true, length = 30)
    private String numeroCargo;

    @Column(name = "ID_ESTUDIANTE", nullable = false)
    private Long estudianteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROCESO_ORIGEN", nullable = false, length = 30)
    private ProcesoOrigen procesoOrigen;

    @Column(name = "ID_REFERENCIA")
    private Long referenciaId;

    @Column(name = "ID_PERIODO", nullable = false)
    private Long periodoId;

    @Column(name = "TOTAL_CREDITOS")
    private Integer totalCreditos;

    @Column(name = "NUMERO_CUOTAS", nullable = false)
    private Integer numeroCuotas;

    @Column(name = "MONTO_TOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "SALDO_PENDIENTE", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoCargo estado = EstadoCargo.EMITIDO;

    @Column(name = "OBSERVACION", length = 500)
    private String observacion;

    @Column(name = "FECHA_EMISION", nullable = false)
    private LocalDateTime fechaEmision;

    /** Bloqueo optimista: dos pagos simultaneos sobre el mismo cargo no se pisan. */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "cargo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroCuota ASC")
    private List<DetalleCargo> cuotas = new ArrayList<>();

    public void agregarCuota(DetalleCargo cuota) {
        cuota.setCargo(this);
        cuotas.add(cuota);
    }

    public BigDecimal montoPagado() {
        return montoTotal.subtract(saldoPendiente);
    }
}
