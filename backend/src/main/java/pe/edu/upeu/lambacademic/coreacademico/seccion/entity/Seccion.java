package pe.edu.upeu.lambacademic.coreacademico.seccion.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;

import java.util.ArrayList;
import java.util.List;

/**
 * Seccion de un curso en un periodo academico (origen: lambacademic), con sus horarios.
 * periodoId y docenteId son ids de otros modulos (Matricula y Base Universal): se validan
 * en el service a traves de sus servicios publicos, nunca con una relacion JPA directa.
 */
@Entity
@Table(name = "SECCIONES")
@Getter
@Setter
@NoArgsConstructor
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_CURSO", nullable = false)
    private Curso curso;

    @Column(name = "ID_PERIODO", nullable = false)
    private Long periodoId;

    @Column(name = "GRUPO", nullable = false, length = 10)
    private String grupo;

    @Column(name = "ID_DOCENTE")
    private Long docenteId;

    @Column(name = "LIMITE_VACANTES", nullable = false)
    private Integer limiteVacantes;

    /**
     * Solo lo modifica la sentencia UPDATE atomica de reserva/liberacion de cupos
     * (SeccionRepository): updatable = false evita que un PUT lo sobrescriba con un valor viejo.
     */
    @Column(name = "CUPOS_OCUPADOS", nullable = false, updatable = false)
    private Integer cuposOcupados = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoSeccion estado = EstadoSeccion.ABIERTA;

    @OneToMany(mappedBy = "seccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<HorarioAula> horarios = new ArrayList<>();

    public void agregarHorario(HorarioAula horario) {
        horario.setSeccion(this);
        horarios.add(horario);
    }

    public int vacantesDisponibles() {
        return limiteVacantes - cuposOcupados;
    }
}
