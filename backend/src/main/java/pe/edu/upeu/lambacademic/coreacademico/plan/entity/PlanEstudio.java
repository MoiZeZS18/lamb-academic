package pe.edu.upeu.lambacademic.coreacademico.plan.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * CABECERA de la malla curricular (origen: LP2_G1). Sus totales se calculan a partir de
 * los detalles, nunca se reciben del cliente. La escuela pertenece a Base Universal: se
 * guarda su id y se valida mediante InstitucionalService (sin relacion JPA entre modulos).
 */
@Entity
@Table(name = "PLAN_ESTUDIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ID_ESCUELA", nullable = false)
    private Long escuelaId;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "ANIO", nullable = false)
    private Integer anio;

    @Builder.Default
    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private Estado estado = Estado.VIGENTE;

    @Builder.Default
    @Column(name = "TOTAL_CREDITOS", nullable = false)
    private Integer totalCreditos = 0;

    @Builder.Default
    @Column(name = "TOTAL_CURSOS", nullable = false)
    private Integer totalCursos = 0;

    @Builder.Default
    @OneToMany(mappedBy = "planEstudio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ciclo ASC, id ASC")
    private List<DetallePlanEstudio> detalles = new ArrayList<>();

    public void addDetalle(DetallePlanEstudio detalle) {
        detalles.add(detalle);
        detalle.setPlanEstudio(this);
    }

    public void recalcularTotales() {
        this.totalCursos = detalles.size();
        this.totalCreditos = detalles.stream()
                .mapToInt(d -> d.getCreditos() == null ? 0 : d.getCreditos())
                .sum();
    }

    public enum Estado { VIGENTE, OBSOLETO }
}
