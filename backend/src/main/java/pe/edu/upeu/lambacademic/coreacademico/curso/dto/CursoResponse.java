package pe.edu.upeu.lambacademic.coreacademico.curso.dto;

public record CursoResponse(
        Long id,
        String codigo,
        String nombre,
        Integer creditos,
        Integer horasTeoricas,
        Integer horasPracticas,
        Boolean activo
) {
}
