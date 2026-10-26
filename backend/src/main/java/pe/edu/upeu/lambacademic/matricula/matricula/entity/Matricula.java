package pe.edu.upeu.lambacademic.matricula.matricula.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MATRICULAS")
@Getter
@Setter
@NoArgsConstructor
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ESTUDIANTE_ID", nullable = false)
    private Long estudianteId;

    @Column(name = "PERIODO_ID", nullable = false)
    private Long periodoId;

    @Column(name = "FECHA", nullable = false)
    private LocalDate fecha;

    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    /** Suma de creditos de los detalles; NULL en matriculas previas a la integracion. */
    @Column(name = "TOTAL_CREDITOS")
    private Integer totalCreditos;

    /**
     * "estudianteId-periodoId" mientras la matricula esta activa; NULL al anularla.
     * UK_MATRICULA_ACTIVA en Oracle impide dos matriculas activas del mismo estudiante
     * en el mismo periodo, incluso si dos peticiones llegan a la vez.
     */
    @Column(name = "CLAVE_ACTIVA", length = 40, unique = true)
    private String claveActiva;

    @Column(name = "FECHA_ANULACION")
    private LocalDateTime fechaAnulacion;

    /** Lotes de carga de detalles: permite paginar en Oracle sin JOIN FETCH. */
    @BatchSize(size = 50)
    @OneToMany(
            mappedBy = "matricula",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DetalleMatricula> detalles = new ArrayList<>();
}