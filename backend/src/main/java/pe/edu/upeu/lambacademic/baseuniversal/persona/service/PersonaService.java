package pe.edu.upeu.lambacademic.baseuniversal.persona.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.repository.AlumnoRepository;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Escuela;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.repository.EscuelaRepository;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.ConteoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;
import pe.edu.upeu.lambacademic.baseuniversal.persona.mapper.PersonaMapper;
import pe.edu.upeu.lambacademic.baseuniversal.persona.repository.PersonaRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Set;

/**
 * CRUD del maestro de personas (base de LP2_G1) y API publica para otros modulos:
 * Core Academico la usa para validar docentes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonaService {

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("apellidoPaterno", "nombres", "numeroDocumento", "tipoPersona", "id", "email");

    private final PersonaRepository repository;
    private final EscuelaRepository escuelaRepository;
    private final AlumnoRepository alumnoRepository;
    private final PersonaMapper mapper;

    @Transactional(readOnly = true)
    public List<PersonaResponse> buscar(Persona.TipoPersona tipoPersona, Boolean activo, String texto,
                                        Long idEscuela, String sortBy, String dir) {
        log.debug("Buscando personas tipo={} activo={} texto={} escuela={} sortBy={} dir={}",
                tipoPersona, activo, texto, idEscuela, sortBy, dir);
        Sort.Direction direccion = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "apellidoPaterno";
        return repository.buscar(tipoPersona, activo, idEscuela, patronLike(texto), Sort.by(direccion, campo))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PersonaResponse obtener(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Transactional(readOnly = true)
    public PersonaResponse obtenerPorDocumento(Persona.TipoDocumento tipoDocumento, String numero) {
        return mapper.toResponse(repository.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numero)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe persona con " + tipoDocumento + " " + numero)));
    }

    /** API publica: plana docente (personas de tipo DOCENTE), ordenada por apellido. */
    @Transactional(readOnly = true)
    public List<PersonaResponse> listarDocentes(Boolean activo, String texto) {
        return buscar(Persona.TipoPersona.DOCENTE, activo, texto, null, "apellidoPaterno", "asc");
    }

    /** API publica: valida que la persona exista, este activa y sea DOCENTE. */
    @Transactional(readOnly = true)
    public PersonaResponse obtenerDocente(Long id) {
        Persona persona = buscarOFallar(id);
        if (persona.getTipoPersona() != Persona.TipoPersona.DOCENTE) {
            throw new ReglaNegocioException("La persona " + id + " no es docente");
        }
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            throw new ReglaNegocioException("El docente " + id + " esta inactivo");
        }
        return mapper.toResponse(persona);
    }

    @Transactional
    public PersonaResponse crear(PersonaRequest request) {
        validarDocumento(request);
        if (repository.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), request.numeroDocumento())) {
            throw new ReglaNegocioException("Ya existe una persona con " + request.tipoDocumento()
                    + " " + request.numeroDocumento());
        }
        Persona persona = mapper.toEntity(request);
        persona.setEscuela(escuelaDe(request.idEscuela()));
        persona.setActivo(Boolean.TRUE);
        Persona guardada = repository.save(persona);
        log.info("Persona creada id={} tipo={}", guardada.getId(), guardada.getTipoPersona());
        return mapper.toResponse(guardada);
    }

    @Transactional
    public PersonaResponse actualizar(Long id, PersonaRequest request) {
        Persona persona = buscarOFallar(id);
        validarDocumento(request);
        if (repository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                request.tipoDocumento(), request.numeroDocumento(), id)) {
            throw new ReglaNegocioException("El documento ya pertenece a otra persona");
        }
        if (persona.getTipoPersona() != request.tipoPersona() && alumnoRepository.existsByPersonaId(id)) {
            throw new ReglaNegocioException("La persona " + id + " esta registrada como alumno; no puede cambiar su tipo a "
                    + request.tipoPersona());
        }
        mapper.actualizar(persona, request);
        persona.setEscuela(escuelaDe(request.idEscuela()));
        log.info("Persona actualizada id={}", id);
        return mapper.toResponse(repository.save(persona));
    }

    /** Borrado logico: una persona con matriculas o cargos nunca se elimina fisicamente. */
    @Transactional
    public void desactivar(Long id) {
        Persona persona = buscarOFallar(id);
        persona.setActivo(Boolean.FALSE);
        repository.save(persona);
        log.info("Persona desactivada id={}", id);
    }

    @Transactional(readOnly = true)
    public List<ConteoResponse> resumenPorTipo() {
        return repository.contarPorTipoPersona().stream()
                .map(fila -> new ConteoResponse(String.valueOf(fila[0]), ((Number) fila[1]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConteoResponse> resumenPorEscuela() {
        return repository.contarPorEscuela().stream()
                .map(fila -> new ConteoResponse(String.valueOf(fila[0]), ((Number) fila[1]).longValue()))
                .toList();
    }

    private Persona buscarOFallar(Long id) {
        return repository.findWithEscuelaById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", id));
    }

    private void validarDocumento(PersonaRequest request) {
        String doc = request.numeroDocumento();
        if (request.tipoDocumento() == Persona.TipoDocumento.DNI && doc.length() != 8) {
            throw new ReglaNegocioException("El DNI debe tener exactamente 8 digitos");
        }
        if (request.tipoDocumento() == Persona.TipoDocumento.RUC && doc.length() != 11) {
            throw new ReglaNegocioException("El RUC debe tener exactamente 11 digitos");
        }
    }

    private Escuela escuelaDe(Long idEscuela) {
        if (idEscuela == null) {
            return null;
        }
        return escuelaRepository.findById(idEscuela)
                .orElseThrow(() -> new ResourceNotFoundException("Escuela", idEscuela));
    }

    static String patronLike(String texto) {
        return texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
    }
}
