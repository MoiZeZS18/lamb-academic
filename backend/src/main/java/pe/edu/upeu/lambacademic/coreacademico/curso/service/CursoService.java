package pe.edu.upeu.lambacademic.coreacademico.curso.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoRequest;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResponse;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;
import pe.edu.upeu.lambacademic.coreacademico.curso.mapper.CursoMapper;
import pe.edu.upeu.lambacademic.coreacademico.curso.repository.CursoRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Set;

/** CRUD completo del catalogo de cursos (integra LP2_G1 y lambacademic). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CursoService {

    private static final Set<String> CAMPOS_ORDENABLES = Set.of("codigo", "nombre", "creditos", "id");

    private final CursoRepository repository;
    private final CursoMapper mapper;

    @Transactional(readOnly = true)
    public List<CursoResponse> buscar(Boolean activo, Integer creditos, String texto, String sortBy, String dir) {
        Sort.Direction direccion = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "codigo";
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
        return repository.buscar(activo, creditos, patron, Sort.by(direccion, campo))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CursoResponse obtener(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public CursoResponse crear(CursoRequest req) {
        if (repository.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("Ya existe un curso con codigo " + req.codigo());
        }
        Curso curso = Curso.builder()
                .codigo(req.codigo())
                .nombre(req.nombre())
                .creditos(req.creditos())
                .horasTeoricas(req.horasTeoricas() == null ? 0 : req.horasTeoricas())
                .horasPracticas(req.horasPracticas() == null ? 0 : req.horasPracticas())
                .activo(Boolean.TRUE)
                .build();
        Curso guardado = repository.save(curso);
        log.info("Curso creado id={} codigo={}", guardado.getId(), guardado.getCodigo());
        return mapper.toResponse(guardado);
    }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest req) {
        Curso curso = buscarOFallar(id);
        if (repository.existsByCodigoAndIdNot(req.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otro curso con codigo " + req.codigo());
        }
        curso.setCodigo(req.codigo());
        curso.setNombre(req.nombre());
        curso.setCreditos(req.creditos());
        curso.setHorasTeoricas(req.horasTeoricas() == null ? 0 : req.horasTeoricas());
        curso.setHorasPracticas(req.horasPracticas() == null ? 0 : req.horasPracticas());
        return mapper.toResponse(repository.save(curso));
    }

    /** Borrado logico: el curso puede estar en mallas, secciones y matriculas historicas. */
    @Transactional
    public void desactivar(Long id) {
        Curso curso = buscarOFallar(id);
        curso.setActivo(Boolean.FALSE);
        repository.save(curso);
        log.info("Curso desactivado id={}", id);
    }

    private Curso buscarOFallar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Curso", id));
    }
}
