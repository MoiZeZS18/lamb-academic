package pe.edu.upeu.lambacademic.coreacademico.plan.dto;

/** Fila del reporte agregado de mallas por escuela y estado. */
public record PlanResumenResponse(
        Long idEscuela,
        String escuela,
        String estado,
        Long cantidadPlanes,
        Long totalCreditos
) {
}
