package pe.edu.upeu.lambacademic.baseuniversal.institucional.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nivel 3 del arbol institucional: escuela profesional de una facultad. */
@Entity
@Table(name = "ESCUELA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escuela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_FACULTAD", nullable = false)
    private Facultad facultad;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "NIVEL", nullable = false, length = 20)
    private Nivel nivel = Nivel.PREGRADO;

    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public enum Nivel { PREGRADO, POSGRADO }
}
