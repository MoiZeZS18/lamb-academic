package pe.edu.upeu.lambacademic.baseuniversal.institucional.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.ArbolResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.FacultadRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.FacultadResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.SedeRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.SedeResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Escuela;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Facultad;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Sede;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.mapper.InstitucionalMapper;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.repository.EscuelaRepository;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.repository.FacultadRepository;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.repository.SedeRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.util.List;

/**
 * CRUD del arbol institucional Sede > Facultad > Escuela (codigo base de LP2_G1).
 * Es el servicio publico del submodulo: Core Academico lo usa para validar escuelas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstitucionalService {

    private final SedeRepository sedeRepository;
    private final FacultadRepository facultadRepository;
    private final EscuelaRepository escuelaRepository;
    private final InstitucionalMapper mapper;

    /* ------------------------------ Sede ------------------------------ */

    @Transactional(readOnly = true)
    public List<SedeResponse> listarSedes() {
        return sedeRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SedeResponse obtenerSede(Long id) {
        return mapper.toResponse(buscarSede(id));
    }

    @Transactional
    public SedeResponse crearSede(SedeRequest req) {
        if (sedeRepository.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("Ya existe una sede con codigo " + req.codigo());
        }
        Sede sede = Sede.builder().codigo(req.codigo()).nombre(req.nombre())
                .direccion(req.direccion()).activo(Boolean.TRUE).build();
        Sede guardada = sedeRepository.save(sede);
        log.info("Sede creada id={} codigo={}", guardada.getId(), guardada.getCodigo());
        return mapper.toResponse(guardada);
    }

    @Transactional
    public SedeResponse actualizarSede(Long id, SedeRequest req) {
        Sede sede = buscarSede(id);
        if (sedeRepository.existsByCodigoAndIdNot(req.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otra sede con codigo " + req.codigo());
        }
        sede.setCodigo(req.codigo());
        sede.setNombre(req.nombre());
        sede.setDireccion(req.direccion());
        return mapper.toResponse(sedeRepository.save(sede));
    }

    /** Borrado logico; una sede con facultades activas no puede desactivarse. */
    @Transactional
    public void desactivarSede(Long id) {
        Sede sede = buscarSede(id);
        if (facultadRepository.existsBySedeIdAndActivoTrue(id)) {
            throw new ReglaNegocioException("La sede " + sede.getCodigo() + " todavia tiene facultades activas");
        }
        sede.setActivo(Boolean.FALSE);
        sedeRepository.save(sede);
        log.info("Sede desactivada id={}", id);
    }

    /* ---------------------------- Facultad ---------------------------- */

    @Transactional(readOnly = true)
    public List<FacultadResponse> listarFacultades(Long idSede) {
        List<Facultad> lista = idSede == null
                ? facultadRepository.findAllByOrderByNombre()
                : facultadRepository.findBySedeIdOrderByNombre(idSede);
        return lista.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FacultadResponse obtenerFacultad(Long id) {
        return mapper.toResponse(buscarFacultad(id));
    }

    @Transactional
    public FacultadResponse crearFacultad(FacultadRequest req) {
        if (facultadRepository.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("Ya existe una facultad con codigo " + req.codigo());
        }
        Sede sede = buscarSedeActiva(req.idSede());
        Facultad facultad = Facultad.builder().sede(sede).codigo(req.codigo()).nombre(req.nombre())
                .activo(Boolean.TRUE).build();
        return mapper.toResponse(facultadRepository.save(facultad));
    }

    @Transactional
    public FacultadResponse actualizarFacultad(Long id, FacultadRequest req) {
        Facultad facultad = buscarFacultad(id);
        if (facultadRepository.existsByCodigoAndIdNot(req.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otra facultad con codigo " + req.codigo());
        }
        facultad.setSede(buscarSedeActiva(req.idSede()));
        facultad.setCodigo(req.codigo());
        facultad.setNombre(req.nombre());
        return mapper.toResponse(facultadRepository.save(facultad));
    }

    @Transactional
    public void desactivarFacultad(Long id) {
        Facultad facultad = buscarFacultad(id);
        if (escuelaRepository.existsByFacultadIdAndActivoTrue(id)) {
            throw new ReglaNegocioException("La facultad " + facultad.getCodigo() + " todavia tiene escuelas activas");
        }
        facultad.setActivo(Boolean.FALSE);
        facultadRepository.save(facultad);
    }

    /* ----------------------------- Escuela ---------------------------- */

    @Transactional(readOnly = true)
    public List<EscuelaResponse> listarEscuelas(Long idFacultad) {
        List<Escuela> lista = idFacultad == null
                ? escuelaRepository.findAllByOrderByNombre()
                : escuelaRepository.findByFacultadIdOrderByNombre(idFacultad);
        return lista.stream().map(mapper::toResponse).toList();
    }

    /** API publica: usada por Core Academico (planes de estudio). Lanza 404 si no existe. */
    @Transactional(readOnly = true)
    public EscuelaResponse obtenerEscuela(Long id) {
        return mapper.toResponse(buscarEscuela(id));
    }

    @Transactional
    public EscuelaResponse crearEscuela(EscuelaRequest req) {
        if (escuelaRepository.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("Ya existe una escuela con codigo " + req.codigo());
        }
        Escuela escuela = Escuela.builder().facultad(buscarFacultadActiva(req.idFacultad()))
                .codigo(req.codigo()).nombre(req.nombre())
                .nivel(req.nivel() == null ? Escuela.Nivel.PREGRADO : req.nivel())
                .activo(Boolean.TRUE).build();
        return mapper.toResponse(escuelaRepository.save(escuela));
    }

    @Transactional
    public EscuelaResponse actualizarEscuela(Long id, EscuelaRequest req) {
        Escuela escuela = buscarEscuela(id);
        if (escuelaRepository.existsByCodigoAndIdNot(req.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otra escuela con codigo " + req.codigo());
        }
        escuela.setFacultad(buscarFacultadActiva(req.idFacultad()));
        escuela.setCodigo(req.codigo());
        escuela.setNombre(req.nombre());
        if (req.nivel() != null) {
            escuela.setNivel(req.nivel());
        }
        return mapper.toResponse(escuelaRepository.save(escuela));
    }

    @Transactional
    public void desactivarEscuela(Long id) {
        Escuela escuela = buscarEscuela(id);
        escuela.setActivo(Boolean.FALSE);
        escuelaRepository.save(escuela);
    }

    /* -------------------------- Arbol completo ------------------------ */

    @Transactional(readOnly = true)
    public List<ArbolResponse> arbol() {
        return sedeRepository.findAll().stream().map(sede -> new ArbolResponse(
                sede.getId(), sede.getCodigo(), sede.getNombre(),
                facultadRepository.findBySedeIdOrderByNombre(sede.getId()).stream()
                        .map(facultad -> new ArbolResponse.FacultadArbol(
                                facultad.getId(), facultad.getCodigo(), facultad.getNombre(),
                                escuelaRepository.findByFacultadIdOrderByNombre(facultad.getId()).stream()
                                        .map(escuela -> new ArbolResponse.EscuelaArbol(escuela.getId(),
                                                escuela.getCodigo(), escuela.getNombre(), escuela.getNivel().name()))
                                        .toList()))
                        .toList()
        )).toList();
    }

    /* ---------------------------- busquedas --------------------------- */

    private Sede buscarSede(Long id) {
        return sedeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede", id));
    }

    private Sede buscarSedeActiva(Long id) {
        Sede sede = buscarSede(id);
        if (!Boolean.TRUE.equals(sede.getActivo())) {
            throw new ReglaNegocioException("La sede " + sede.getCodigo() + " esta inactiva");
        }
        return sede;
    }

    private Facultad buscarFacultad(Long id) {
        return facultadRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Facultad", id));
    }

    private Facultad buscarFacultadActiva(Long id) {
        Facultad facultad = buscarFacultad(id);
        if (!Boolean.TRUE.equals(facultad.getActivo())) {
            throw new ReglaNegocioException("La facultad " + facultad.getCodigo() + " esta inactiva");
        }
        return facultad;
    }

    private Escuela buscarEscuela(Long id) {
        return escuelaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Escuela", id));
    }
}
