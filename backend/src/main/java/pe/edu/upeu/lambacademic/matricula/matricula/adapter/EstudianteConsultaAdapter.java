package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import org.springframework.stereotype.Component;
import pe.edu.upeu.lambacademic.matricula.matricula.port.EstudianteConsultaPort;

@Component
public class EstudianteConsultaAdapter implements EstudianteConsultaPort {

    @Override
    public EstudianteInfo consultarEstudiante(Long estudianteId) {

        return new EstudianteInfo(
                estudianteId,
                true,
                true
        );
    }
}