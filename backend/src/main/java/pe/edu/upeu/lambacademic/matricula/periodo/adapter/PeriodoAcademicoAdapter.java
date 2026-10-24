package pe.edu.upeu.lambacademic.matricula.periodo.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.coreacademico.seccion.port.PeriodoAcademicoPort;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.util.Optional;

/**
 * Matricula es duenio del Periodo y lo ofrece a Core Academico implementando el puerto que
 * Core declara (inversion de dependencias: evita el ciclo Matricula -> Core -> Matricula).
 */
@Component
@RequiredArgsConstructor
public class PeriodoAcademicoAdapter implements PeriodoAcademicoPort {

    private final PeriodoRepository periodoRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PeriodoInfo> buscarPeriodo(Long periodoId) {
        if (periodoId == null) {
            return Optional.empty();
        }
        return periodoRepository.findById(periodoId)
                .map(p -> new PeriodoInfo(p.getId(), p.getCodigo(), p.getNombre(), p.getEstado()));
    }
}
