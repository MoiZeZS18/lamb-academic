package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.coreacademico.seccion.port.SeccionEnUsoPort;
import pe.edu.upeu.lambacademic.matricula.matricula.repository.DetalleMatriculaRepository;

/** Matricula le informa a Core Academico si una seccion aparece en algun detalle (incluidas anuladas). */
@Component
@RequiredArgsConstructor
public class SeccionEnUsoAdapter implements SeccionEnUsoPort {

    private final DetalleMatriculaRepository detalleMatriculaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean tieneMatriculas(Long seccionId) {
        return detalleMatriculaRepository.existsBySeccionId(seccionId);
    }
}
