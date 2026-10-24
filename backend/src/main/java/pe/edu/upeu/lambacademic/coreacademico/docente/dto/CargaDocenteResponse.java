package pe.edu.upeu.lambacademic.coreacademico.docente.dto;

import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionResponse;

import java.util.List;

/** Carga lectiva de un docente en un periodo: secciones asignadas y horas semanales. */
public record CargaDocenteResponse(
        Long docenteId,
        String docenteNombre,
        Long periodoId,
        Integer totalSecciones,
        Double horasSemanales,
        List<SeccionResponse> secciones
) {
}
