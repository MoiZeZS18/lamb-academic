package pe.edu.upeu.lambacademic.matricula.periodo.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.finanzas.cargo.port.PeriodoFinancieroPort;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.util.Optional;

/** Ofrece el Periodo a Finanzas mediante el puerto que Finanzas declara (sin ciclo de modulos). */
@Component
@RequiredArgsConstructor
public class PeriodoFinancieroAdapter implements PeriodoFinancieroPort {

    private final PeriodoRepository periodoRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PeriodoInfo> buscarPeriodo(Long periodoId) {
        if (periodoId == null) {
            return Optional.empty();
        }
        return periodoRepository.findById(periodoId)
                .map(p -> new PeriodoInfo(p.getId(), p.getCodigo(), p.getNombre()));
    }
}
