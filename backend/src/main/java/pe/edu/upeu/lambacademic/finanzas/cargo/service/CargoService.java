package pe.edu.upeu.lambacademic.finanzas.cargo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.service.AlumnoService;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoEstadoReporte;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoGenerado;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoMatriculaCommand;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoRequest;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResumenResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.PagoRequest;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.PagoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.SaldoEstudianteResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Cargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.DetalleCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCuota;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Pago;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;
import pe.edu.upeu.lambacademic.finanzas.cargo.mapper.CargoMapper;
import pe.edu.upeu.lambacademic.finanzas.cargo.port.PeriodoFinancieroPort;
import pe.edu.upeu.lambacademic.finanzas.cargo.repository.CargoRepository;
import pe.edu.upeu.lambacademic.finanzas.cargo.repository.PagoRepository;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;
import pe.edu.upeu.lambacademic.finanzas.concepto.repository.ConceptoPagoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cargos al estudiante (origen: Frinanzas-lamb), corregido e integrado:
 * - monto calculado desde ConceptoPago (no constantes en codigo);
 * - cuotas que suman exactamente el total (sin error de redondeo);
 * - idempotencia por (proceso origen, referencia): un solo cargo por matricula;
 * - pagos con bloqueo optimista; deuda que excluye cargos anulados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CargoService {

    /** Dia de vencimiento mensual de cada cuota (regla del modulo original). */
    static final int DIA_VENCIMIENTO = 10;

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> CAMPOS_ORDENABLES = Set.of("fechaEmision", "montoTotal", "saldoPendiente", "id");

    private final CargoRepository cargoRepository;
    private final PagoRepository pagoRepository;
    private final ConceptoPagoRepository conceptoRepository;
    private final CargoMapper mapper;
    private final AlumnoService alumnoService;
    private final PeriodoFinancieroPort periodoPort;

    /* ------------------------ API publica para Matricula ------------------------ */

    /**
     * Genera (o devuelve, si ya existe) el cargo de una matricula. Se une a la transaccion
     * de la matricula: si algo falla, no queda ni la matricula ni el cargo.
     */
    @Transactional
    public CargoGenerado generarCargoMatricula(CargoMatriculaCommand comando) {
        Optional<Cargo> existente = cargoRepository.findByProcesoOrigenAndReferenciaId(
                ProcesoOrigen.MATRICULA, comando.matriculaId());
        if (existente.isPresent()) {
            Cargo cargo = existente.get();
            log.info("Cargo de la matricula {} ya existia ({}); no se duplica", comando.matriculaId(),
                    cargo.getNumeroCargo());
            return toGenerado(cargo, false);
        }
        List<ConceptoPago> conceptos = conceptoRepository.findByActivoTrueAndAplicaMatriculaTrueOrderByCodigo();
        Cargo cargo = construirCargo(comando.estudianteId(), ProcesoOrigen.MATRICULA, comando.matriculaId(),
                comando.periodoId(), comando.totalCreditos(), comando.numeroCuotas(),
                "Matricula " + comando.matriculaId() + " - periodo " + comando.periodoCodigo(), conceptos,
                "Matricula " + comando.periodoCodigo());
        Cargo guardado = cargoRepository.save(cargo);
        log.info("Cargo {} generado para la matricula {}: total={} cuotas={}", guardado.getNumeroCargo(),
                comando.matriculaId(), guardado.getMontoTotal(), guardado.getNumeroCuotas());
        return toGenerado(guardado, true);
    }

    /** Anula el cargo asociado a una matricula; falla si ya tiene pagos. */
    @Transactional
    public void anularCargoDeMatricula(Long matriculaId) {
        cargoRepository.findByProcesoOrigenAndReferenciaId(ProcesoOrigen.MATRICULA, matriculaId)
                .ifPresent(this::anularCargo);
    }

    /* ------------------------------ Cargos manuales ----------------------------- */

    @Transactional
    public CargoResponse registrarCargo(CargoRequest request) {
        // Auditoria: un cargo manual de MATRICULA (sobre todo sin referencia) duplicaria la deuda
        // que la matricula ya genera automaticamente en su propia transaccion.
        if (request.procesoOrigen() == ProcesoOrigen.MATRICULA) {
            throw new ReglaNegocioException("Los cargos de matricula se generan al registrar la matricula "
                    + "(o con POST /api/v1/matriculas/{id}/cargo); no se registran manualmente");
        }
        alumnoService.buscarEstudiante(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", request.estudianteId()));
        PeriodoFinancieroPort.PeriodoInfo periodo = periodoPort.buscarPeriodo(request.periodoId())
                .orElseThrow(() -> new ResourceNotFoundException("Periodo", request.periodoId()));
        if (request.referenciaId() != null && cargoRepository
                .findByProcesoOrigenAndReferenciaId(request.procesoOrigen(), request.referenciaId()).isPresent()) {
            throw new ReglaNegocioException("Ya existe un cargo para " + request.procesoOrigen()
                    + " con referencia " + request.referenciaId());
        }
        List<ConceptoPago> conceptos = resolverConceptos(request.conceptos());
        String etiqueta = request.procesoOrigen().name() + " " + periodo.codigo();
        Cargo cargo = construirCargo(request.estudianteId(), request.procesoOrigen(), request.referenciaId(),
                periodo.id(), request.totalCreditos(), request.numeroCuotas(), request.observacion(), conceptos,
                etiqueta);
        Cargo guardado = cargoRepository.save(cargo);
        log.info("Cargo manual {} registrado: estudiante={} total={}", guardado.getNumeroCargo(),
                guardado.getEstudianteId(), guardado.getMontoTotal());
        return mapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public CargoResponse obtener(Long id) {
        return mapper.toResponse(buscarOFallar(id));
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarDeEstudiante(Long estudianteId) {
        return cargoRepository.findByEstudianteIdOrderByFechaEmisionDesc(estudianteId).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CargoResumenResponse> buscar(Long estudianteId, Long periodoId, EstadoCargo estado,
                                             ProcesoOrigen procesoOrigen, String sortBy, String dir) {
        Sort.Direction direccion = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String campo = CAMPOS_ORDENABLES.contains(sortBy) ? sortBy : "fechaEmision";
        return cargoRepository.buscar(estudianteId, periodoId, estado, procesoOrigen, Sort.by(direccion, campo));
    }

    @Transactional(readOnly = true)
    public SaldoEstudianteResponse saldoDeEstudiante(Long estudianteId) {
        alumnoService.buscarEstudiante(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", estudianteId));
        Object[] fila = cargoRepository.saldoDeEstudiante(estudianteId, EstadoCargo.EMITIDO,
                EstadoCargo.PAGADO_PARCIAL, EstadoCargo.ANULADO).getFirst();
        BigDecimal deuda = (BigDecimal) fila[0];
        BigDecimal saldo = (BigDecimal) fila[1];
        int pendientes = fila[2] == null ? 0 : ((Number) fila[2]).intValue();
        long vencidas = cargoRepository.cuotasVencidas(estudianteId, EstadoCuota.PENDIENTE,
                EstadoCuota.PAGADA_PARCIAL, LocalDate.now());
        return new SaldoEstudianteResponse(estudianteId, deuda, deuda.subtract(saldo), saldo, pendientes, vencidas);
    }

    @Transactional(readOnly = true)
    public List<CargoEstadoReporte> resumen(Long periodoId) {
        return cargoRepository.resumenPorEstado(periodoId);
    }

    @Transactional
    public CargoResponse anular(Long id) {
        Cargo cargo = buscarOFallar(id);
        anularCargo(cargo);
        return mapper.toResponse(cargo);
    }

    /* ----------------------------------- Pagos ---------------------------------- */

    /**
     * Registra un pago y lo aplica a las cuotas en orden de vencimiento. El @Version del
     * cargo impide que dos pagos simultaneos calculen el saldo sobre el mismo valor viejo.
     */
    @Transactional
    public CargoResponse registrarPago(Long cargoId, PagoRequest request) {
        Cargo cargo = buscarOFallar(cargoId);
        if (cargo.getEstado() == EstadoCargo.ANULADO) {
            throw new ReglaNegocioException("El cargo " + cargo.getNumeroCargo() + " esta anulado");
        }
        if (cargo.getEstado() == EstadoCargo.PAGADO_TOTAL) {
            throw new ReglaNegocioException("El cargo " + cargo.getNumeroCargo() + " ya esta pagado");
        }
        BigDecimal monto = request.monto().setScale(CalculadoraCargo.ESCALA);
        if (monto.compareTo(cargo.getSaldoPendiente()) > 0) {
            throw new ReglaNegocioException("El pago (" + monto + ") supera el saldo pendiente ("
                    + cargo.getSaldoPendiente() + ")");
        }

        BigDecimal porAplicar = monto;
        for (DetalleCargo cuota : cargo.getCuotas()) {
            if (porAplicar.signum() == 0) {
                break;
            }
            BigDecimal saldoCuota = cuota.saldo();
            if (saldoCuota.signum() == 0) {
                continue;
            }
            BigDecimal aplicado = porAplicar.min(saldoCuota);
            cuota.setMontoPagado(cuota.getMontoPagado().add(aplicado));
            cuota.setEstado(cuota.saldo().signum() == 0 ? EstadoCuota.PAGADA : EstadoCuota.PAGADA_PARCIAL);
            porAplicar = porAplicar.subtract(aplicado);
        }
        cargo.setSaldoPendiente(cargo.getSaldoPendiente().subtract(monto));
        cargo.setEstado(cargo.getSaldoPendiente().signum() == 0 ? EstadoCargo.PAGADO_TOTAL : EstadoCargo.PAGADO_PARCIAL);

        Pago pago = new Pago();
        pago.setCargo(cargo);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setMedioPago(request.medioPago());
        pago.setReferencia(request.referencia());
        pagoRepository.save(pago);
        Cargo guardado = cargoRepository.saveAndFlush(cargo);
        log.info("Pago de {} aplicado al cargo {}; saldo={}", monto, cargo.getNumeroCargo(),
                guardado.getSaldoPendiente());
        return mapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos(Long cargoId) {
        buscarOFallar(cargoId);
        return pagoRepository.findByCargoIdOrderByFechaPagoAsc(cargoId).stream().map(mapper::toResponse).toList();
    }

    /* --------------------------------- internos --------------------------------- */

    private Cargo construirCargo(Long estudianteId, ProcesoOrigen origen, Long referenciaId, Long periodoId,
                                 Integer creditos, Integer numeroCuotas, String observacion,
                                 List<ConceptoPago> conceptos, String etiquetaCuota) {
        if (conceptos.isEmpty()) {
            throw new ReglaNegocioException("No hay conceptos de pago activos configurados para el cargo");
        }
        BigDecimal total = CalculadoraCargo.montoTotal(conceptos, creditos);
        if (total.signum() <= 0) {
            throw new ReglaNegocioException("El monto del cargo debe ser mayor a cero");
        }
        int cuotas = numeroCuotas == null ? 1 : numeroCuotas;
        List<BigDecimal> montos = CalculadoraCargo.fraccionar(total, cuotas);

        Cargo cargo = new Cargo();
        cargo.setNumeroCargo(nuevoNumeroCargo());
        cargo.setEstudianteId(estudianteId);
        cargo.setProcesoOrigen(origen);
        cargo.setReferenciaId(referenciaId);
        cargo.setPeriodoId(periodoId);
        cargo.setTotalCreditos(creditos);
        cargo.setNumeroCuotas(cuotas);
        cargo.setMontoTotal(total);
        cargo.setSaldoPendiente(total);
        cargo.setEstado(EstadoCargo.EMITIDO);
        cargo.setObservacion(observacion);
        LocalDateTime ahora = LocalDateTime.now();
        cargo.setFechaEmision(ahora);
        for (int i = 0; i < cuotas; i++) {
            DetalleCargo cuota = new DetalleCargo();
            cuota.setNumeroCuota(i + 1);
            cuota.setConceptoNombre("Cuota " + (i + 1) + "/" + cuotas + " - " + etiquetaCuota);
            cuota.setMontoCuota(montos.get(i));
            cuota.setMontoPagado(BigDecimal.ZERO.setScale(CalculadoraCargo.ESCALA));
            cuota.setEstado(EstadoCuota.PENDIENTE);
            cuota.setFechaVencimiento(ahora.toLocalDate().plusMonths(i + 1L).withDayOfMonth(DIA_VENCIMIENTO));
            cargo.agregarCuota(cuota);
        }
        return cargo;
    }

    private void anularCargo(Cargo cargo) {
        if (cargo.getEstado() == EstadoCargo.ANULADO) {
            return;
        }
        if (cargo.montoPagado().signum() > 0 || pagoRepository.existsByCargoId(cargo.getId())) {
            throw new ReglaNegocioException("El cargo " + cargo.getNumeroCargo()
                    + " tiene pagos registrados y no puede anularse");
        }
        cargo.setEstado(EstadoCargo.ANULADO);
        cargo.setSaldoPendiente(BigDecimal.ZERO.setScale(CalculadoraCargo.ESCALA));
        cargo.getCuotas().forEach(c -> c.setEstado(EstadoCuota.ANULADA));
        cargoRepository.save(cargo);
        log.info("Cargo {} anulado", cargo.getNumeroCargo());
    }

    private List<ConceptoPago> resolverConceptos(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return conceptoRepository.findByActivoTrueAndAplicaMatriculaTrueOrderByCodigo();
        }
        Set<String> unicos = new HashSet<>(codigos);
        if (unicos.size() != codigos.size()) {
            throw new ReglaNegocioException("Hay conceptos de pago repetidos en la solicitud");
        }
        List<ConceptoPago> conceptos = conceptoRepository.findByCodigoIn(unicos);
        if (conceptos.size() != unicos.size()) {
            throw new ResourceNotFoundException("Uno o mas conceptos de pago no existen: " + codigos);
        }
        conceptos.stream().filter(c -> !Boolean.TRUE.equals(c.getActivo())).findFirst().ifPresent(c -> {
            throw new ReglaNegocioException("El concepto " + c.getCodigo() + " esta inactivo");
        });
        return conceptos;
    }

    private static String nuevoNumeroCargo() {
        return "CRG-" + LocalDateTime.now().format(SELLO) + "-"
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }

    private static CargoGenerado toGenerado(Cargo cargo, boolean nuevo) {
        return new CargoGenerado(cargo.getId(), cargo.getNumeroCargo(), cargo.getMontoTotal(),
                cargo.getNumeroCuotas(), cargo.getEstado().name(), nuevo);
    }

    private Cargo buscarOFallar(Long id) {
        return cargoRepository.findDetalladoById(id).orElseThrow(() -> new ResourceNotFoundException("Cargo", id));
    }
}
