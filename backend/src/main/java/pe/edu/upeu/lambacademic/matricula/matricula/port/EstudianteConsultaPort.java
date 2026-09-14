package pe.edu.upeu.lambacademic.matricula.matricula.port;

public interface EstudianteConsultaPort {

    EstudianteInfo consultarEstudiante(Long estudianteId);

    record EstudianteInfo(
            Long estudianteId,
            boolean existe,
            boolean habilitado
    ) {
    }
}