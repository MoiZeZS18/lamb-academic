package pe.edu.upeu.lambacademic.finanzas.cargo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.service.AlumnoService;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoGenerado;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoMatriculaCommand;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.PagoRequest;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Cargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.DetalleCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCuota;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.MedioPago;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;
import pe.edu.upeu.lambacademic.finanzas.cargo.mapper.CargoMapper;
import pe.edu.upeu.lambacademic.finanzas.cargo.port.PeriodoFinancieroPort;
import pe.edu.upeu.lambacademic.finanzas.cargo.repository.CargoRepository;
import pe.edu.upeu.lambacademic.finanzas.cargo.repository.PagoRepository;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;
import pe.edu.upeu.lambacademic.finanzas.concepto.repository.ConceptoPagoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CargoServiceTest {

    @Mock private CargoRepository cargoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ConceptoPagoRepository conceptoRepository;
    @Mock private AlumnoService alumnoService;
    @Mock private PeriodoFinancieroPort periodoPort;
    @Spy private CargoMapper mapper = Mappers.getMapper(CargoMapper.class);

    @InjectMocks private CargoService service;

    @BeforeEach
    void setUp() {
        ConceptoPago base = new ConceptoPago();
        base.setCodigo("MAT-BASE");
        base.setTipoCalculo(ConceptoPago.TipoCalculo.FIJO);
        base.setMonto(new BigDecimal("150.00"));
        ConceptoPago credito = new ConceptoPago();
        credito.setCodigo("CRED");
        credito.setTipoCalculo(ConceptoPago.TipoCalculo.POR_CREDITO);
        credito.setMonto(new BigDecimal("50.00"));
        when(conceptoRepository.findByActivoTrueAndAplicaMatriculaTrueOrderByCodigo()).thenReturn(List.of(base, credito));
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cargoRepository.saveAndFlush(any(Cargo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Genera el cargo de matricula: 150 + 50 x 20 = 1150 en 3 cuotas exactas")
    void generaCargoDeMatricula() {
        when(cargoRepository.findByProcesoOrigenAndReferenciaId(ProcesoOrigen.MATRICULA, 10L)).thenReturn(Optional.empty());

        CargoGenerado generado = service.generarCargoMatricula(new CargoMatriculaCommand(10L, 1001L, 4L, "2026-II", 20, 3));

        assertThat(generado.nuevo()).isTrue();
        assertThat(generado.montoTotal()).isEqualByComparingTo("1150.00");
        assertThat(generado.numeroCuotas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Idempotencia: si la matricula ya tiene cargo, se devuelve el mismo y no se crea otro")
    void noDuplicaCargo() {
        Cargo existente = cargo("700.00", "700.00");
        when(cargoRepository.findByProcesoOrigenAndReferenciaId(ProcesoOrigen.MATRICULA, 10L))
                .thenReturn(Optional.of(existente));

        CargoGenerado generado = service.generarCargoMatricula(new CargoMatriculaCommand(10L, 1001L, 4L, "2026-II", 11, 5));

        assertThat(generado.nuevo()).isFalse();
        assertThat(generado.numeroCargo()).isEqualTo("CRG-TEST");
        verify(cargoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un pago se aplica a las cuotas en orden y actualiza estados y saldo")
    void pagoParcialSeAplicaEnOrden() {
        Cargo cargo = cargo("300.00", "300.00");
        when(cargoRepository.findDetalladoById(1L)).thenReturn(Optional.of(cargo));

        CargoResponse respuesta = service.registrarPago(1L, new PagoRequest(new BigDecimal("150.00"), MedioPago.EFECTIVO, null));

        assertThat(respuesta.saldoPendiente()).isEqualByComparingTo("150.00");
        assertThat(respuesta.estado()).isEqualTo(EstadoCargo.PAGADO_PARCIAL);
        assertThat(cargo.getCuotas().get(0).getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(cargo.getCuotas().get(1).getEstado()).isEqualTo(EstadoCuota.PAGADA_PARCIAL);
        assertThat(cargo.getCuotas().get(2).getEstado()).isEqualTo(EstadoCuota.PENDIENTE);
    }

    @Test
    void pagoMayorAlSaldoSeRechaza() {
        when(cargoRepository.findDetalladoById(1L)).thenReturn(Optional.of(cargo("300.00", "300.00")));
        assertThatThrownBy(() -> service.registrarPago(1L, new PagoRequest(new BigDecimal("300.01"), MedioPago.EFECTIVO, null)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("supera el saldo");
    }

    @Test
    void cargoConPagosNoSeAnula() {
        when(cargoRepository.findDetalladoById(1L)).thenReturn(Optional.of(cargo("300.00", "200.00")));
        assertThatThrownBy(() -> service.anular(1L))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("pagos");
    }

    @Test
    void anulacionDejaSaldoEnCero() {
        Cargo cargo = cargo("300.00", "300.00");
        when(cargoRepository.findDetalladoById(1L)).thenReturn(Optional.of(cargo));
        CargoResponse respuesta = service.anular(1L);
        assertThat(respuesta.estado()).isEqualTo(EstadoCargo.ANULADO);
        assertThat(respuesta.saldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(cargo.getCuotas()).allSatisfy(c -> assertThat(c.getEstado()).isEqualTo(EstadoCuota.ANULADA));
    }

    private static Cargo cargo(String total, String saldo) {
        Cargo cargo = new Cargo();
        cargo.setId(1L);
        cargo.setNumeroCargo("CRG-TEST");
        cargo.setEstudianteId(1001L);
        cargo.setProcesoOrigen(ProcesoOrigen.MATRICULA);
        cargo.setPeriodoId(4L);
        cargo.setNumeroCuotas(3);
        cargo.setMontoTotal(new BigDecimal(total));
        cargo.setSaldoPendiente(new BigDecimal(saldo));
        cargo.setEstado(EstadoCargo.EMITIDO);
        List<BigDecimal> montos = CalculadoraCargo.fraccionar(new BigDecimal(total), 3);
        for (int i = 1; i <= 3; i++) {
            DetalleCargo cuota = new DetalleCargo();
            cuota.setNumeroCuota(i);
            cuota.setMontoCuota(montos.get(i - 1));
            cuota.setMontoPagado(new BigDecimal("0.00"));
            cuota.setEstado(EstadoCuota.PENDIENTE);
            cargo.agregarCuota(cuota);
        }
        return cargo;
    }
}
