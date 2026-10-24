package pe.edu.upeu.lambacademic.coreacademico.curso.dto;

/** DTO relacionado reducido: lo embeben Seccion y DetallePlanEstudio. */
public record CursoResumen(Long id, String codigo, String nombre, Integer creditos) {
}
