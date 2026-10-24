package pe.edu.upeu.lambacademic.matricula.matricula.dto;

public record DetalleMatriculaResponse(
        Long id,
        Long seccionId,
        Long cursoId,
        Integer creditos
) {
}