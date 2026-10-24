package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lambacademic.coreacademico.plan.service.PlanEstudioService;
import pe.edu.upeu.lambacademic.matricula.matricula.port.PrerrequisitoConsultaPort;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PrerrequisitoConsultaAdapter implements PrerrequisitoConsultaPort {

    private final PlanEstudioService planEstudioService;

    @Override
    public List<PrerrequisitoInfo> prerrequisitosDe(Long cursoId, Long escuelaId) {
        return planEstudioService.prerrequisitosDeCurso(cursoId, escuelaId).stream()
                .map(c -> new PrerrequisitoInfo(c.id(), c.codigo()))
                .toList();
    }
}
