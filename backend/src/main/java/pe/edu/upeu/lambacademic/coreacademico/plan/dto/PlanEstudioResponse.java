package pe.edu.upeu.lambacademic.coreacademico.plan.dto;

import pe.edu.upeu.lambacademic.coreacademico.plan.entity.DetallePlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.PlanEstudio;

import java.util.List;

public record PlanEstudioResponse(
        Long id,
        Long idEscuela,
        String escuelaNombre,
        String codigo,
        String nombre,
        Integer anio,
        Integer version,
        PlanEstudio.Estado estado,
        Integer totalCreditos,
        Integer totalCursos,
        List<DetallePlanResponse> detalles
) {

    public record DetallePlanResponse(
            Long id,
            Long idCurso,
            String cursoCodigo,
            String cursoNombre,
            Integer ciclo,
            Integer creditos,
            DetallePlanEstudio.Tipo tipo,
            Long idPrerrequisito,
            String prerrequisitoCodigo
    ) {
    }
}
