package pe.edu.upeu.lambacademic.matricula.matricula.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DETALLE_MATRICULA",
        uniqueConstraints = @UniqueConstraint(name = "UK_DET_MATRICULA_SECCION",
                columnNames = {"MATRICULA_ID", "SECCION_ID"}))
@Getter
@Setter
@NoArgsConstructor
public class DetalleMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "MATRICULA_ID", nullable = false)
    private Matricula matricula;

    @Column(name = "SECCION_ID", nullable = false)
    private Long seccionId;

    /** Copia del curso y sus creditos al momento de matricular (registro historico, S4 2.2). */
    @Column(name = "CURSO_ID")
    private Long cursoId;

    @Column(name = "CREDITOS")
    private Integer creditos;

    /** Resultado final del curso (V4); NULL mientras no se registre. */
    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", length = 15)
    private ResultadoCurso resultado;

    @Column(name = "FECHA_RESULTADO")
    private LocalDateTime fechaResultado;
}