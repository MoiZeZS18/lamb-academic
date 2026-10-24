package pe.edu.upeu.lambacademic.coreacademico.plan.dto;

/** Reporte: cantidad de cursos y creditos en cada ciclo de una malla. */
public record PlanDistribucionResponse(Integer ciclo, Long cantidadCursos, Long creditos) {
}
