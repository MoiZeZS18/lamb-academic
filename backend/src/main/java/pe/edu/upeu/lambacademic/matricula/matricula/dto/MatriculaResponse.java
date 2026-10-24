package pe.edu.upeu.lambacademic.matricula.matricula.dto;

import java.time.LocalDate;
import java.util.List;

public record MatriculaResponse(
        Long id,
        Long estudianteId,
        Long periodoId,
        LocalDate fecha,
        String estado,
        List<DetalleMatriculaResponse> detalles,
        Integer totalCreditos,
        CargoMatriculaResponse cargo
) {

    public MatriculaResponse conCargo(CargoMatriculaResponse nuevoCargo) {
        return new MatriculaResponse(id, estudianteId, periodoId, fecha, estado, detalles, totalCreditos, nuevoCargo);
    }
}