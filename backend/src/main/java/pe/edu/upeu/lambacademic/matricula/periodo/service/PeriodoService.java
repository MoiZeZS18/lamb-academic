package pe.edu.upeu.lambacademic.matricula.periodo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.repository.MatriculaRepository;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoResponse;
import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;
import pe.edu.upeu.lambacademic.matricula.periodo.mapper.PeriodoMapper;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.util.List;

/**
 * CRUD de periodos academicos: fechas validas, codigo unico y 404 si no existe. No se elimina un
 * periodo con matriculas; si tiene secciones, lo impide ademas la FK SECCIONES.ID_PERIODO en Oracle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodoService {

    private final PeriodoRepository periodoRepository;
    private final PeriodoMapper periodoMapper;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public List<PeriodoResponse> listar() {
        return periodoRepository.findAll()
                .stream()
                .map(periodoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PeriodoResponse obtenerPorId(Long id) {
        return periodoMapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public PeriodoResponse crear(PeriodoRequest request) {
        validarFechas(request);
        if (periodoRepository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException("Ya existe un periodo con codigo " + request.codigo());
        }
        Periodo periodo = periodoMapper.toEntity(request);
        Periodo guardado = periodoRepository.save(periodo);
        log.info("Periodo creado id={} codigo={}", guardado.getId(), guardado.getCodigo());
        return periodoMapper.toResponse(guardado);
    }

    @Transactional
    public PeriodoResponse actualizar(Long id, PeriodoRequest request) {
        Periodo periodo = buscarOFallar(id);
        validarFechas(request);
        if (periodoRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otro periodo con codigo " + request.codigo());
        }

        periodo.setCodigo(request.codigo());
        periodo.setNombre(request.nombre());
        periodo.setFechaInicio(request.fechaInicio());
        periodo.setFechaFin(request.fechaFin());
        periodo.setEstado(request.estado());

        Periodo actualizado = periodoRepository.save(periodo);

        return periodoMapper.toResponse(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        Periodo periodo = buscarOFallar(id);
        if (matriculaRepository.existsByPeriodoId(id)) {
            throw new ReglaNegocioException("El periodo " + periodo.getCodigo()
                    + " tiene matriculas registradas y no puede eliminarse");
        }
        periodoRepository.delete(periodo);
    }

    private void validarFechas(PeriodoRequest request) {
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }

    private Periodo buscarOFallar(Long id) {
        return periodoRepository.findById(id)
                .orElseThrow(() -> new PeriodoNoEncontradoException("El periodo " + id + " no existe"));
    }
}
