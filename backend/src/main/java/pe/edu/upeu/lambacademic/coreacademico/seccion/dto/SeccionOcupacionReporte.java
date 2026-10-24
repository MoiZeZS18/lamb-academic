package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

/** Fila del reporte de ocupacion por curso en un periodo. */
public record SeccionOcupacionReporte(
        String cursoCodigo,
        String cursoNombre,
        Long secciones,
        Long vacantesTotales,
        Long cuposOcupados
) {
}
