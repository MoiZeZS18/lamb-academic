package pe.edu.upeu.lambacademic.matricula.periodo.dto;

import java.time.LocalDate;

public record PeriodoResponse(
        Long id,
        String codigo,
        String nombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {
}