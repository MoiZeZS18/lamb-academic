package pe.edu.upeu.lambacademic.baseuniversal.alumno.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;

import java.time.LocalDate;

/**
 * Condicion de estudiante de una Persona (origen: LP2_L). Su id es el "estudianteId"
 * que usan Matricula y Finanzas.
 */
@Entity
@Table(name = "ALUMNOS")
@Getter
@Setter
@NoArgsConstructor
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PERSONA", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "CODIGO_ESTUDIANTE", nullable = false, unique = true, length = 20)
    private String codigoEstudiante;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoAlumno estado = EstadoAlumno.ACTIVO;

    @Column(name = "FECHA_INGRESO", nullable = false)
    private LocalDate fechaIngreso;

    /** Puede matricularse si su condicion es ACTIVO y la persona sigue activa. */
    public boolean estaHabilitado() {
        return estado == EstadoAlumno.ACTIVO && persona != null && Boolean.TRUE.equals(persona.getActivo());
    }
}
