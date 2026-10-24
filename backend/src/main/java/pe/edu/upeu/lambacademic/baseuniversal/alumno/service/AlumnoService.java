package pe.edu.upeu.lambacademic.baseuniversal.alumno.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.AlumnoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.AlumnoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.EstudianteResumen;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.Alumno;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.EstadoAlumno;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.mapper.AlumnoMapper;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.repository.AlumnoRepository;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.ConteoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;
import pe.edu.upeu.lambacademic.baseuniversal.persona.repository.PersonaRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * CRUD de Alumno (origen: LP2_L) y API publica de estudiantes para Matricula y Finanzas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlumnoService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "codigoEstudiante", "codigoEstudiante",
            "fechaIngreso", "fechaIngreso",
            "estado", "estado",
            "id", "id",
            "apellidoPaterno", "persona.apellidoPaterno");

    /** Transiciones permitidas de la condicion del estudiante. */
    private static final Map<EstadoAlumno, Set<EstadoAlumno>> TRANSICIONES = Map.of(
            EstadoAlumno.ACTIVO, Set.of(EstadoAlumno.SUSPENDIDO, EstadoAlumno.RETIRADO, EstadoAlumno.EGRESADO),
            EstadoAlumno.SUSPENDIDO, Set.of(EstadoAlumno.ACTIVO, EstadoAlumno.RETIRADO),
            EstadoAlumno.RETIRADO, Set.of(EstadoAlumno.ACTIVO),
            EstadoAlumno.EGRESADO, Set.of());

    private final AlumnoRepository alumnoRepository;
    private final PersonaRepository personaRepository;
    private final AlumnoMapper mapper;

    @Transactional(readOnly = true)
    public List<AlumnoResponse> buscar(EstadoAlumno estado, Long idEscuela, String texto, String sortBy, String dir) {
        Sort.Direction direccion = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = CAMPOS_ORDENABLES.getOrDefault(sortBy, "codigoEstudiante");
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
        return alumnoRepository.buscar(estado, idEscuela, patron, Sort.by(direccion, campo))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AlumnoResponse obtener(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    /**
     * API publica (Matricula, Finanzas). Optional vacio si el estudiante no existe;
     * la decision de que hacer en ese caso la toma el modulo que consulta.
     */
    @Transactional(readOnly = true)
    public Optional<EstudianteResumen> buscarEstudiante(Long estudianteId) {
        if (estudianteId == null) {
            return Optional.empty();
        }
        return alumnoRepository.findWithPersonaById(estudianteId).map(mapper::toEstudianteResumen);
    }

    @Transactional
    public AlumnoResponse crear(AlumnoRequest request) {
        Persona persona = personaRepository.findWithEscuelaById(request.personaId())
                .orElseThrow(() -> new ResourceNotFoundException("Persona", request.personaId()));
        if (persona.getTipoPersona() != Persona.TipoPersona.ALUMNO) {
            throw new ReglaNegocioException("La persona " + persona.getId() + " es de tipo "
                    + persona.getTipoPersona() + "; solo una persona ALUMNO puede registrarse como estudiante");
        }
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            throw new ReglaNegocioException("La persona " + persona.getId() + " esta inactiva");
        }
        if (alumnoRepository.existsByPersonaId(persona.getId())) {
            throw new ReglaNegocioException("La persona " + persona.getId() + " ya esta registrada como alumno");
        }
        if (alumnoRepository.existsByCodigoEstudiante(request.codigoEstudiante())) {
            throw new ReglaNegocioException("El codigo de estudiante " + request.codigoEstudiante() + " ya existe");
        }
        Alumno alumno = new Alumno();
        alumno.setPersona(persona);
        alumno.setCodigoEstudiante(request.codigoEstudiante());
        alumno.setFechaIngreso(request.fechaIngreso());
        alumno.setEstado(EstadoAlumno.ACTIVO);
        Alumno guardado = alumnoRepository.save(alumno);
        log.info("Alumno creado id={} codigo={} personaId={}", guardado.getId(), guardado.getCodigoEstudiante(),
                persona.getId());
        return mapper.toResponse(guardado);
    }

    /** Actualiza codigo y fecha de ingreso; la persona asociada no cambia. */
    @Transactional
    public AlumnoResponse actualizar(Long id, AlumnoRequest request) {
        Alumno alumno = buscarOFallar(id);
        if (!alumno.getPersona().getId().equals(request.personaId())) {
            throw new ReglaNegocioException("No se puede cambiar la persona asociada a un alumno");
        }
        if (alumnoRepository.existsByCodigoEstudianteAndIdNot(request.codigoEstudiante(), id)) {
            throw new ReglaNegocioException("El codigo de estudiante " + request.codigoEstudiante() + " ya existe");
        }
        alumno.setCodigoEstudiante(request.codigoEstudiante());
        alumno.setFechaIngreso(request.fechaIngreso());
        return mapper.toResponse(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse cambiarEstado(Long id, EstadoAlumno nuevo) {
        Alumno alumno = buscarOFallar(id);
        if (alumno.getEstado() == nuevo) {
            return mapper.toResponse(alumno);
        }
        if (!TRANSICIONES.get(alumno.getEstado()).contains(nuevo)) {
            throw new ReglaNegocioException("No se permite pasar de " + alumno.getEstado() + " a " + nuevo);
        }
        log.info("Alumno id={} cambia de estado {} -> {}", id, alumno.getEstado(), nuevo);
        alumno.setEstado(nuevo);
        return mapper.toResponse(alumnoRepository.save(alumno));
    }

    @Transactional(readOnly = true)
    public List<ConteoResponse> resumenPorEstado() {
        return alumnoRepository.contarPorEstado().stream()
                .map(fila -> new ConteoResponse(String.valueOf(fila[0]), ((Number) fila[1]).longValue()))
                .toList();
    }

    private Alumno buscarOFallar(Long id) {
        return alumnoRepository.findWithPersonaById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno", id));
    }
}
