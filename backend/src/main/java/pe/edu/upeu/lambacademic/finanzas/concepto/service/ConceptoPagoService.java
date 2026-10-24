package pe.edu.upeu.lambacademic.finanzas.concepto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;
import pe.edu.upeu.lambacademic.finanzas.concepto.dto.ConceptoPagoRequest;
import pe.edu.upeu.lambacademic.finanzas.concepto.dto.ConceptoPagoResponse;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;
import pe.edu.upeu.lambacademic.finanzas.concepto.mapper.ConceptoPagoMapper;
import pe.edu.upeu.lambacademic.finanzas.concepto.repository.ConceptoPagoRepository;

import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptoPagoService {

    private final ConceptoPagoRepository repository;
    private final ConceptoPagoMapper mapper;

    @Transactional(readOnly = true)
    public List<ConceptoPagoResponse> listar() {
        return repository.findAllByOrderByCodigo().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConceptoPagoResponse obtener(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public ConceptoPagoResponse crear(ConceptoPagoRequest request) {
        if (repository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException("Ya existe un concepto de pago con codigo " + request.codigo());
        }
        ConceptoPago concepto = new ConceptoPago();
        aplicar(concepto, request);
        concepto.setActivo(Boolean.TRUE);
        ConceptoPago guardado = repository.save(concepto);
        log.info("Concepto de pago creado id={} codigo={} monto={}", guardado.getId(), guardado.getCodigo(),
                guardado.getMonto());
        return mapper.toResponse(guardado);
    }

    /** El cambio de tarifa solo afecta cargos futuros: los emitidos guardan su monto. */
    @Transactional
    public ConceptoPagoResponse actualizar(Long id, ConceptoPagoRequest request) {
        ConceptoPago concepto = buscarOFallar(id);
        if (repository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe otro concepto de pago con codigo " + request.codigo());
        }
        aplicar(concepto, request);
        return mapper.toResponse(repository.save(concepto));
    }

    @Transactional
    public void desactivar(Long id) {
        ConceptoPago concepto = buscarOFallar(id);
        concepto.setActivo(Boolean.FALSE);
        repository.save(concepto);
    }

    private void aplicar(ConceptoPago concepto, ConceptoPagoRequest request) {
        concepto.setCodigo(request.codigo());
        concepto.setNombre(request.nombre());
        concepto.setTipoCalculo(request.tipoCalculo());
        concepto.setMonto(request.monto().setScale(2, RoundingMode.HALF_UP));
        concepto.setAplicaMatricula(Boolean.TRUE.equals(request.aplicaMatricula()));
    }

    private ConceptoPago buscarOFallar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Concepto de pago", id));
    }
}
