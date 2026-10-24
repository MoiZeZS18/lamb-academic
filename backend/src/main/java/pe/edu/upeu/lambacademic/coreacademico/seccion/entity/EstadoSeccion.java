package pe.edu.upeu.lambacademic.coreacademico.seccion.entity;

public enum EstadoSeccion {
    /** Acepta matriculas mientras tenga vacantes. */
    ABIERTA,
    /** Programada, pero ya no acepta matriculas nuevas. */
    CERRADA,
    /** No se dictara; solo posible si no tiene matriculados. */
    CANCELADA
}
