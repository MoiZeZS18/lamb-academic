package pe.edu.upeu.lambacademic.coreacademico.seccion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import pe.edu.upeu.lambacademic.compartido.persistencia.HoraConverter;

import java.time.LocalTime;

/** Bloque horario de una seccion en un aula (detalle de la programacion). */
@Entity
@Table(name = "HORARIOS_AULA")
@Getter
@Setter
@NoArgsConstructor
public class HorarioAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_SECCION", nullable = false)
    private Seccion seccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIA_SEMANA", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Convert(converter = HoraConverter.class)
    @Column(name = "HORA_INICIO", nullable = false, length = 5)
    private LocalTime horaInicio;

    @Convert(converter = HoraConverter.class)
    @Column(name = "HORA_FIN", nullable = false, length = 5)
    private LocalTime horaFin;

    @Column(name = "AULA", nullable = false, length = 50)
    private String aula;

    public boolean seCruzaCon(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        return diaSemana == dia && horaInicio.isBefore(fin) && inicio.isBefore(horaFin);
    }
}
