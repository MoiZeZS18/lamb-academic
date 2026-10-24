package pe.edu.upeu.lambacademic.baseuniversal.institucional.dto;

public record FacultadResponse(
        Long id,
        Long idSede,
        String sedeNombre,
        String codigo,
        String nombre,
        Boolean activo
) {
}
