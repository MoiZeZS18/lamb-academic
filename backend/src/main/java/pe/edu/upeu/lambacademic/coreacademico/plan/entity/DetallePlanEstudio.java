package pe.edu.upeu.lambacademic.coreacademico.plan.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;

/**
 * DETALLE de la malla: un curso en un ciclo del plan, con su prerrequisito opcional
 * (otro curso del mismo plan, de un ciclo anterior).
 */
@Entity
@Table(name = "DETALLE_PLAN_ESTUDIO",
        uniqueConstraints = @UniqueConstraint(name = "UK_DET_PLAN_CURSO", columnNames = {"ID_PLAN", "ID_CURSO"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetallePlanEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PLAN", nullable = false)
    private PlanEstudio planEstudio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_CURSO", nullable = false)
    private Curso curso;

    @Column(name = "CICLO", nullable = false)
    private Integer ciclo;

    @Column(name = "CREDITOS", nullable = false)
    private Integer creditos;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private Tipo tipo = Tipo.OBLIGATORIO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PRERREQUISITO")
    private Curso prerrequisito;

    public enum Tipo { OBLIGATORIO, ELECTIVO }
}
