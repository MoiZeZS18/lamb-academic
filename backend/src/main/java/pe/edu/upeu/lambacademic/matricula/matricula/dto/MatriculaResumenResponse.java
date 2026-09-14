package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import java.time.LocalDate;

public record MatriculaResumenResponse(
        Long id,
        Long estudianteId,
        Long periodoId,
        LocalDate fecha,
        String estado,
        Long totalDetalles
) {
}