package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResumen;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;

import java.util.List;

public record SeccionResponse(
        Long id,
        CursoResumen curso,
        Long periodoId,
        String grupo,
        Long docenteId,
        String docenteNombre,
        Integer limiteVacantes,
        Integer cuposOcupados,
        Integer vacantesDisponibles,
        EstadoSeccion estado,
        List<HorarioAulaResponse> horarios
) {
}
