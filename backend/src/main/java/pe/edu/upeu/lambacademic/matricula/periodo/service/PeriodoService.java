package pe.edu.upeu.lambacademic.matricula.periodo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoResponse;
import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;
import pe.edu.upeu.lambacademic.matricula.periodo.mapper.PeriodoMapper;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodoService {

    private final PeriodoRepository periodoRepository;
    private final PeriodoMapper periodoMapper;

    public List<PeriodoResponse> listar() {
        return periodoRepository.findAll()
                .stream()
                .map(periodoMapper::toResponse)
                .toList();
    }

    public PeriodoResponse obtenerPorId(Long id) {
        Periodo periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado"));

        return periodoMapper.toResponse(periodo);
    }

    public PeriodoResponse crear(PeriodoRequest request) {
        Periodo periodo = periodoMapper.toEntity(request);
        Periodo guardado = periodoRepository.save(periodo);

        return periodoMapper.toResponse(guardado);
    }

    public PeriodoResponse actualizar(Long id, PeriodoRequest request) {
        Periodo periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado"));

        periodo.setCodigo(request.codigo());
        periodo.setNombre(request.nombre());
        periodo.setFechaInicio(request.fechaInicio());
        periodo.setFechaFin(request.fechaFin());
        periodo.setEstado(request.estado());

        Periodo actualizado = periodoRepository.save(periodo);

        return periodoMapper.toResponse(actualizado);
    }

    public void eliminar(Long id) {
        Periodo periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado"));

        periodoRepository.delete(periodo);
    }
}