package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import org.springframework.stereotype.Component;
import pe.edu.upeu.lambacademic.matricula.matricula.port.SeccionConsultaPort;

import java.util.List;

@Component
public class SeccionConsultaAdapter implements SeccionConsultaPort {

    @Override
    public SeccionInfo consultarSeccion(Long seccionId) {

        return new SeccionInfo(
                seccionId,
                30,
                30
        );
    }

    @Override
    public List<HorarioInfo> consultarHorarios(Long seccionId) {

        return List.of();
    }
}