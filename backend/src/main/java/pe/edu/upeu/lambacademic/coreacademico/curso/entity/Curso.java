package pe.edu.upeu.lambacademic.coreacademico.curso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catalogo unico de cursos. Unifica el Curso de LP2_G1 (malla curricular) y el de
 * lambacademic (oferta): ambos usaban el mismo concepto con columnas distintas.
 */
@Entity
@Table(name = "CURSO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "CREDITOS", nullable = false)
    private Integer creditos;

    @Builder.Default
    @Column(name = "HORAS_TEORICAS", nullable = false)
    private Integer horasTeoricas = 0;

    @Builder.Default
    @Column(name = "HORAS_PRACTICAS", nullable = false)
    private Integer horasPracticas = 0;

    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;
}
