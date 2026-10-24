package pe.edu.upeu.lambacademic.baseuniversal.expediente.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.DocumentoAdjuntoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.DocumentoAdjuntoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteEstadoReporte;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteRequest;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteResponse;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteResumen;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.DocumentoAdjunto;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.EstadoExpediente;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.Expediente;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.mapper.ExpedienteMapper;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.repository.ExpedienteRepository;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;
import pe.edu.upeu.lambacademic.baseuniversal.persona.repository.PersonaRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operacion cabecera-detalle Expediente - DocumentoAdjunto (origen: LP2_L, reescrita).
 * registrar() es atomico: si un documento viola una regla, no queda ni la cabecera
 * ni ningun documento (rollback por excepcion no controlada, S4 2.5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private static final int MAX_DOCUMENTOS = 20;

    private static final Set<String> CAMPOS_ORDENABLES = Set.of("fechaRegistro", "codigoTramite", "estado", "id");

    private static final Map<EstadoExpediente, Set<EstadoExpediente>> TRANSICIONES = Map.of(
            EstadoExpediente.REGISTRADO, Set.of(EstadoExpediente.EN_REVISION, EstadoExpediente.ARCHIVADO),
            EstadoExpediente.EN_REVISION, Set.of(EstadoExpediente.OBSERVADO, EstadoExpediente.APROBADO),
            EstadoExpediente.OBSERVADO, Set.of(EstadoExpediente.EN_REVISION, EstadoExpediente.ARCHIVADO),
            EstadoExpediente.APROBADO, Set.of(EstadoExpediente.ARCHIVADO),
            EstadoExpediente.ARCHIVADO, Set.of());

    private final ExpedienteRepository expedienteRepository;
    private final PersonaRepository personaRepository;
    private final ExpedienteMapper mapper;

    @Transactional
    public ExpedienteResponse registrar(ExpedienteRequest request) {
        if (expedienteRepository.existsByCodigoTramite(request.codigoTramite())) {
            throw new ReglaNegocioException("Ya existe un expediente con codigo " + request.codigoTramite());
        }
        Persona persona = personaRepository.findWithEscuelaById(request.personaId())
                .orElseThrow(() -> new ResourceNotFoundException("Persona", request.personaId()));
        if (!Boolean.TRUE.equals(persona.getActivo())) {
            throw new ReglaNegocioException("La persona " + persona.getId() + " esta inactiva");
        }

        Expediente expediente = new Expediente();
        expediente.setCodigoTramite(request.codigoTramite());
        expediente.setAsunto(request.asunto());
        expediente.setPersona(persona);
        expediente.setFechaRegistro(LocalDateTime.now());
        expediente.setEstado(EstadoExpediente.REGISTRADO);

        Set<String> archivos = new HashSet<>();
        for (DocumentoAdjuntoRequest documento : request.documentos()) {
            validarDocumento(documento, archivos);
            expediente.agregarDocumento(nuevoDocumento(documento));
        }

        Expediente guardado = expedienteRepository.save(expediente);
        log.info("Expediente registrado id={} codigo={} documentos={}", guardado.getId(),
                guardado.getCodigoTramite(), guardado.getTotalDocumentos());
        return mapper.toResponse(guardado);
    }

    /** Agrega un documento a un expediente que todavia admite cambios. */
    @Transactional
    public ExpedienteResponse agregarDocumento(Long id, DocumentoAdjuntoRequest request) {
        Expediente expediente = buscarOFallar(id);
        if (expediente.getEstado() != EstadoExpediente.REGISTRADO
                && expediente.getEstado() != EstadoExpediente.OBSERVADO) {
            throw new ReglaNegocioException("El expediente en estado " + expediente.getEstado()
                    + " no admite nuevos documentos");
        }
        if (expediente.getDocumentos().size() >= MAX_DOCUMENTOS) {
            throw new ReglaNegocioException("El expediente ya tiene el maximo de " + MAX_DOCUMENTOS + " documentos");
        }
        Set<String> archivos = new HashSet<>();
        expediente.getDocumentos().forEach(d -> archivos.add(d.getNombreArchivo().toLowerCase()));
        validarDocumento(request, archivos);
        expediente.agregarDocumento(nuevoDocumento(request));
        return mapper.toResponse(expedienteRepository.save(expediente));
    }

    @Transactional
    public ExpedienteResponse cambiarEstado(Long id, EstadoExpediente nuevo) {
        Expediente expediente = buscarOFallar(id);
        if (!TRANSICIONES.get(expediente.getEstado()).contains(nuevo)) {
            throw new ReglaNegocioException("No se permite pasar el expediente de "
                    + expediente.getEstado() + " a " + nuevo);
        }
        log.info("Expediente id={} cambia de estado {} -> {}", id, expediente.getEstado(), nuevo);
        expediente.setEstado(nuevo);
        return mapper.toResponse(expedienteRepository.save(expediente));
    }

    @Transactional(readOnly = true)
    public List<ExpedienteResumen> buscar(Long personaId, EstadoExpediente estado, LocalDateTime desde,
                                          LocalDateTime hasta, String sortBy, String dir) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("El rango de fechas es invalido: 'desde' es posterior a 'hasta'");
        }
        Sort.Direction direccion = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "fechaRegistro";
        return expedienteRepository.buscar(personaId, estado, desde, hasta, Sort.by(direccion, campo));
    }

    @Transactional(readOnly = true)
    public ExpedienteResponse obtener(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Transactional(readOnly = true)
    public List<DocumentoAdjuntoResponse> listarDocumentos(Long id) {
        return buscarOFallar(id).getDocumentos().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpedienteEstadoReporte> resumen() {
        return expedienteRepository.resumenPorEstado();
    }

    private void validarDocumento(DocumentoAdjuntoRequest documento, Set<String> archivosRegistrados) {
        if (!documento.tipoDocumento().coincideCon(documento.nombreArchivo())) {
            throw new ReglaNegocioException("El archivo " + documento.nombreArchivo()
                    + " no corresponde al tipo " + documento.tipoDocumento());
        }
        if (!archivosRegistrados.add(documento.nombreArchivo().toLowerCase())) {
            throw new ReglaNegocioException("El archivo " + documento.nombreArchivo()
                    + " esta repetido en el expediente");
        }
    }

    private DocumentoAdjunto nuevoDocumento(DocumentoAdjuntoRequest request) {
        DocumentoAdjunto documento = mapper.toEntity(request);
        documento.setFechaCarga(LocalDateTime.now());
        return documento;
    }

    private Expediente buscarOFallar(Long id) {
        return expedienteRepository.findDetalladoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente", id));
    }
}
