package pe.edu.upeu.lambacademic.integracion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.service.AlumnoService;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.service.InstitucionalService;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.coreacademico.curso.service.CursoService;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioRequest;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.DetallePlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.service.PlanEstudioService;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.service.CargoService;
import pe.edu.upeu.lambacademic.finanzas.concepto.dto.ConceptoPagoResponse;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;
import pe.edu.upeu.lambacademic.finanzas.concepto.service.ConceptoPagoService;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CupoInsuficienteException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.MatriculaDuplicadaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PrerrequisitoNoCumplidoException;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.ResultadoCurso;
import pe.edu.upeu.lambacademic.matricula.matricula.service.MatriculaService;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.service.PeriodoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integracion de punta a punta contra el Oracle real (esquema LAMB_APP): Base Universal,
 * Core Academico, Matricula y Finanzas en una sola transaccion. Sin @Transactional en la
 * prueba: cada operacion hace commit o rollback real, como en produccion.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MatriculaOracleIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PersonaService personaService;
    @Autowired private AlumnoService alumnoService;
    @Autowired private CursoService cursoService;
    @Autowired private SeccionService seccionService;
    @Autowired private InstitucionalService institucionalService;
    @Autowired private PlanEstudioService planEstudioService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private CargoService cargoService;
    @Autowired private ConceptoPagoService conceptoPagoService;
    @Autowired private PeriodoService periodoService;

    private FixturesOracle fx;
    private Long periodo;
    private Long ana;
    private Long beto;
    private Long carla;
    private Long cursoA;
    private Long seccionA;
    private Long seccionB;
    private Long seccionUnica;
    private Long matriculaAna;

    @BeforeAll
    void crearDatos() {
        fx = new FixturesOracle(jdbc, personaService, alumnoService, cursoService, seccionService, institucionalService);
        periodo = fx.periodoActivo();
        ana = fx.alumno();
        beto = fx.alumno();
        carla = fx.alumno();
        cursoA = fx.curso(4);
        Long cursoB = fx.curso(3);
        Long cursoC = fx.curso(2);
        seccionA = fx.seccion(cursoA, periodo, 5, 7);
        seccionB = fx.seccion(cursoB, periodo, 1, 9);
        seccionUnica = fx.seccion(cursoC, periodo, 1, 11);
    }

    @AfterAll
    void limpiarDatos() {
        fx.limpiar();
    }

    @Test
    @Order(1)
    @DisplayName("Registro completo: cabecera + 2 detalles, cupos reservados y cargo con cuotas exactas")
    void registroCompletoGeneraCargo() {
        MatriculaResponse matricula = matriculaService.registrar(request(ana, 4), detalles(seccionA, seccionB));
        matriculaAna = matricula.id();

        assertThat(matricula.totalCreditos()).isEqualTo(7);
        assertThat(matricula.detalles()).hasSize(2).allSatisfy(d -> assertThat(d.cursoId()).isNotNull());
        assertThat(fx.cuposOcupados(seccionA)).isEqualTo(1);
        assertThat(fx.cuposOcupados(seccionB)).isEqualTo(1);

        BigDecimal esperado = montoEsperado(7);
        assertThat(matricula.cargo().montoTotal()).isEqualByComparingTo(esperado);
        CargoResponse cargo = cargoService.obtener(matricula.cargo().cargoId());
        assertThat(cargo.cuotas()).hasSize(4);
        assertThat(cargo.cuotas().stream().map(c -> c.montoCuota()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(esperado);

        // Regresion: el GET y el listado devolvian "cargo": null aunque el cargo existiera.
        int cargosAntes = fx.cargosDe(ana);
        assertThat(matriculaService.obtener(matriculaAna).cargo().cargoId()).isEqualTo(matricula.cargo().cargoId());
        assertThat(matriculaService.buscar(ana, periodo, null, 0, 10, "fecha", "desc").getContent())
                .singleElement().satisfies(m -> assertThat(m.cargo().cargoId()).isEqualTo(matricula.cargo().cargoId()));
        assertThat(fx.cargosDe(ana)).as("las consultas no generan cargos").isEqualTo(cargosAntes);
    }

    @Test
    @Order(2)
    @DisplayName("Una segunda matricula activa del mismo estudiante en el periodo se rechaza")
    void matriculaDuplicadaSeRechaza() {
        assertThatThrownBy(() -> matriculaService.registrar(request(ana, null), detalles(seccionUnica)))
                .isInstanceOf(MatriculaDuplicadaException.class);
        assertThat(fx.matriculasDe(ana)).isEqualTo(1);
        assertThat(fx.cuposOcupados(seccionUnica)).isZero();
    }

    @Test
    @Order(3)
    @DisplayName("Seccion llena: se rechaza y la otra seccion de la solicitud no queda reservada")
    void cupoAgotadoNoDejaNadaRegistrado() {
        assertThatThrownBy(() -> matriculaService.registrar(request(beto, null), detalles(seccionA, seccionB)))
                .isInstanceOf(CupoInsuficienteException.class);
        assertThat(fx.matriculasDe(beto)).isZero();
        assertThat(fx.cargosDe(beto)).isZero();
        assertThat(fx.cuposOcupados(seccionA)).isEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("Concurrencia: dos estudiantes compiten por el ultimo cupo; solo uno lo obtiene")
    void soloUnoObtieneElUltimoCupo() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<MatriculaResponse>> resultados = new ArrayList<>();
        for (Long alumno : List.of(beto, carla)) {
            Callable<MatriculaResponse> tarea = () -> {
                salida.await();
                return matriculaService.registrar(request(alumno, 1), detalles(seccionUnica));
            };
            resultados.add(hilos.submit(tarea));
        }
        salida.countDown();

        int exitos = 0;
        int rechazos = 0;
        for (Future<MatriculaResponse> resultado : resultados) {
            try {
                resultado.get();
                exitos++;
            } catch (java.util.concurrent.ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOf(ReglaNegocioException.class);
                rechazos++;
            }
        }
        hilos.shutdown();

        assertThat(exitos).isEqualTo(1);
        assertThat(rechazos).isEqualTo(1);
        assertThat(fx.cuposOcupados(seccionUnica)).isEqualTo(1);
        assertThat(fx.matriculasDe(beto) + fx.matriculasDe(carla)).isEqualTo(1);
        assertThat(fx.cargosDe(beto) + fx.cargosDe(carla)).isEqualTo(1);
    }

    @Test
    @Order(5)
    @DisplayName("Anular: libera cupos, anula el cargo y permite volver a matricularse")
    void anulacionLiberaCuposYCargo() {
        MatriculaResponse anulada = matriculaService.anular(matriculaAna);

        assertThat(anulada.estado()).isEqualTo("ANULADA");
        assertThat(fx.cuposOcupados(seccionA)).isZero();
        assertThat(fx.cuposOcupados(seccionB)).isZero();
        assertThat(cargoService.listarDeEstudiante(ana)).singleElement()
                .extracting(CargoResponse::estado).isEqualTo(EstadoCargo.ANULADO);

        MatriculaResponse nueva = matriculaService.registrar(request(ana, 2), detalles(seccionA));
        assertThat(nueva.id()).isNotEqualTo(matriculaAna);
        assertThat(fx.cuposOcupados(seccionA)).isEqualTo(1);
    }

    @Test
    @Order(6)
    @DisplayName("Prerrequisitos: sin curso previo se rechaza; con resultado APROBADO se admite; DESAPROBADO no")
    void prerrequisitoNoCumplido() {
        Long cursoPrevio = fx.curso(3);
        Long cursoAvanzado = fx.curso(3);
        Long plan = planEstudioService.crear(new PlanEstudioRequest(fx.escuela(), "IT-PLAN-" + fx.sufijo,
                "Plan de integracion", 2026, List.of(
                new PlanEstudioRequest.DetallePlanRequest(cursoPrevio, 1, 6, DetallePlanEstudio.Tipo.OBLIGATORIO, null),
                new PlanEstudioRequest.DetallePlanRequest(cursoAvanzado, 2, 6, DetallePlanEstudio.Tipo.OBLIGATORIO,
                        cursoPrevio)))).id();
        fx.registrarPlan(plan);
        Long seccionAvanzada = fx.seccion(cursoAvanzado, periodo, 10, 13);

        Long dani = fx.alumno();
        assertThatThrownBy(() -> matriculaService.registrar(request(dani, null), detalles(seccionAvanzada)))
                .isInstanceOf(PrerrequisitoNoCumplidoException.class);
        assertThat(fx.cuposOcupados(seccionAvanzada)).isZero();

        // A1: solo un prerrequisito APROBADO en un periodo anterior habilita el curso avanzado.
        Long periodoPrevio = fx.periodoPasado(periodoService);
        Long seccionPrevia = fx.seccion(cursoPrevio, periodoPrevio, 10, 14);
        Long eli = fx.alumno();
        Long fer = fx.alumno();
        MatriculaResponse matriculaEli = matriculaService.registrar(
                new MatriculaRequest(eli, periodoPrevio, LocalDate.of(2025, 4, 1), "INICIADA", 1), detalles(seccionPrevia));
        MatriculaResponse matriculaFer = matriculaService.registrar(
                new MatriculaRequest(fer, periodoPrevio, LocalDate.of(2025, 4, 1), "INICIADA", 1), detalles(seccionPrevia));
        Long detalleEli = matriculaEli.detalles().getFirst().id();
        Long detalleFer = matriculaFer.detalles().getFirst().id();

        assertThatThrownBy(() -> matriculaService.registrarResultado(matriculaEli.id(), detalleEli, ResultadoCurso.APROBADO))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("periodo CERRADO");

        var previo = periodoService.obtenerPorId(periodoPrevio);
        periodoService.actualizar(periodoPrevio, new PeriodoRequest(previo.codigo(), previo.nombre(),
                previo.fechaInicio(), previo.fechaFin(), "CERRADO"));
        matriculaService.registrarResultado(matriculaEli.id(), detalleEli, ResultadoCurso.APROBADO);
        matriculaService.registrarResultado(matriculaFer.id(), detalleFer, ResultadoCurso.DESAPROBADO);

        assertThat(matriculaService.registrar(request(eli, 1), detalles(seccionAvanzada)).id()).isNotNull();
        assertThatThrownBy(() -> matriculaService.registrar(request(fer, 1), detalles(seccionAvanzada)))
                .isInstanceOf(PrerrequisitoNoCumplidoException.class)
                .hasMessageContaining("haber aprobado antes");
        assertThat(fx.cuposOcupados(seccionAvanzada)).isEqualTo(1);
    }

    private BigDecimal montoEsperado(int creditos) {
        BigDecimal total = BigDecimal.ZERO;
        for (ConceptoPagoResponse c : conceptoPagoService.listar()) {
            if (Boolean.TRUE.equals(c.activo()) && Boolean.TRUE.equals(c.aplicaMatricula())) {
                total = total.add(c.tipoCalculo() == ConceptoPago.TipoCalculo.FIJO
                        ? c.monto() : c.monto().multiply(BigDecimal.valueOf(creditos)));
            }
        }
        return total;
    }

    private MatriculaRequest request(Long alumno, Integer cuotas) {
        return new MatriculaRequest(alumno, periodo, LocalDate.now(), "INICIADA", cuotas);
    }

    private static List<DetalleMatriculaRequest> detalles(Long... secciones) {
        return java.util.Arrays.stream(secciones).map(DetalleMatriculaRequest::new).toList();
    }
}
