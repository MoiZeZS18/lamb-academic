package pe.edu.upeu.lambacademic.baseuniversal.persona.entity;

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
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Escuela;

import java.time.LocalDate;

/**
 * Maestro unico de personas: alumnos, docentes, administrativos, cajeros.
 * Unifica el modelo de LP2_G1 (tipo/numero de documento, apellidos separados, escuela)
 * con el de LP2_L (dni, nombres, apellidos, email), que era un subconjunto.
 */
@Entity
@Table(name = "PERSONA", uniqueConstraints = @UniqueConstraint(name = "UK_PERSONA_DOC",
        columnNames = {"TIPO_DOCUMENTO", "NUMERO_DOCUMENTO"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_PERSONA", nullable = false, length = 20)
    private TipoPersona tipoPersona;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_DOCUMENTO", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "NUMERO_DOCUMENTO", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "NOMBRES", nullable = false, length = 120)
    private String nombres;

    @Column(name = "APELLIDO_PATERNO", nullable = false, length = 80)
    private String apellidoPaterno;

    @Column(name = "APELLIDO_MATERNO", length = 80)
    private String apellidoMaterno;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "DIRECCION", length = 255)
    private String direccion;

    @Column(name = "FECHA_NACIMIENTO")
    private LocalDate fechaNacimiento;

    /** Escuela a la que pertenece (relacion dentro del mismo modulo Base Universal). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESCUELA")
    private Escuela escuela;

    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public String nombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (apellidoPaterno != null) sb.append(apellidoPaterno).append(' ');
        if (apellidoMaterno != null) sb.append(apellidoMaterno).append(' ');
        if (nombres != null) sb.append(nombres);
        return sb.toString().trim();
    }

    public enum TipoPersona { ALUMNO, DOCENTE, ADMINISTRATIVO, CAJERO, EXTERNO }

    public enum TipoDocumento { DNI, CE, PASAPORTE, RUC }
}
