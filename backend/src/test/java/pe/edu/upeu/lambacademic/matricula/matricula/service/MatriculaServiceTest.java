package pe.edu.upeu.lambacademic.matricula.matricula.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mapstruct.factory.Mappers;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.DetalleMatricula;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.Matricula;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.ResultadoCurso;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CruceHorarioException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CupoInsuficienteException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CursoDuplicadoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.EstudianteNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.EstudianteNoHabilitadoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.MatriculaDuplicadaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoActivoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PeriodoNoEncontradoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.PrerrequisitoNoCumplidoException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionDuplicadaException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionNoDisponibleException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.SeccionNoEncontradaException;
import pe.edu.upeu.lambacademic.matricula.matricula.mapper.DetalleMatriculaMapper;
import pe.edu.upeu.lambacademic.matricula.matricula.mapper.MatriculaMapper;
import pe.edu.upeu.lambacademic.matricula.matricula.port.CargoFinancieroPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.EstudianteConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.PrerrequisitoConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.port.SeccionConsultaPort;
import pe.edu.upeu.lambacademic.matricula.matricula.repository.MatriculaRepository;
import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;
import pe.edu.upeu.lambacademic.matricula.periodo.repository.PeriodoRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas del registro de matricula, con los modulos vecinos simulados por sus puertos. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatriculaServiceTest {

    private static final Long PERIODO = 4L;
    private static final Long ESTUDIANTE = 1001L;
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 24);

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private PeriodoRepository periodoRepository;
    @Mock private SeccionConsultaPort seccionPort;
    @Mock private EstudianteConsultaPort estudiantePort;
    @Mock private PrerrequisitoConsultaPort prerrequisitoPort;
    @Mock private CargoFinancieroPort cargoPort;
    @Spy private MatriculaMapper matriculaMapper = Mappers.getMapper(MatriculaMapper.class);

    @InjectMocks private MatriculaService service;

    @BeforeEach
    void setUp() throws Exception {
        // MapStruct genera MatriculaMapperImpl con un campo detalleMatriculaMapper inyectable.
        Field campo = matriculaMapper.getClass().getDeclaredField("detalleMatriculaMapper");
        campo.setAccessible(true);
        campo.set(matriculaMapper, Mappers.getMapper(DetalleMatriculaMapper.class));

        when(periodoRepository.findById(PERIODO)).thenReturn(Optional.of(periodo("ACTIVO")));
        when(estudiantePort.consultarEstudiante(ESTUDIANTE))
                .thenReturn(new EstudianteConsultaPort.EstudianteInfo(ESTUDIANTE, true, true, 1L));
        when(matriculaRepository.existsByClaveActiva(anyString())).thenReturn(false);
        when(prerrequisitoPort.prerrequisitosDe(anyLong(), any())).thenReturn(List.of());
        when(matriculaRepository.saveAndFlush(any(Matricula.class))).thenAnswer(inv -> {
            Matricula m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });
        when(cargoPort.generarCargo(anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(new CargoFinancieroPort.CargoInfo(7L, "CRG-1", new BigDecimal("700.00"), 5, "EMITIDO"));
        seccion(1001L, 1L, "IS101", 4, PERIODO, 10, true, horario("LUNES", 8, 10));
        seccion(1002L, 2L, "IS102", 4, PERIODO, 10, true, horario("MARTES", 8, 10));
        seccion(1003L, 3L, "IS103", 3, PERIODO, 10, true, horario("LUNES", 10, 12));
    }

    @Test
    @DisplayName("Exito: guarda cabecera + detalles con creditos, reserva cupos y genera el cargo")
    void registraMatriculaCompleta() {
        MatriculaResponse respuesta = service.registrar(request(), detalles(1001L, 1002L, 1003L));

        assertThat(respuesta.id()).isEqualTo(99L);
        assertThat(respuesta.totalCreditos()).isEqualTo(11);
        assertThat(respuesta.detalles()).hasSize(3);
        assertThat(respuesta.cargo().numeroCargo()).isEqualTo("CRG-1");
        verify(seccionPort).reservarCupo(1001L);
        verify(seccionPort).reservarCupo(1002L);
        verify(seccionPort).reservarCupo(1003L);
        verify(cargoPort).generarCargo(99L, ESTUDIANTE, PERIODO, "2026-II", 11, 5);
    }

    @Test
    void periodoInexistente() {
        when(periodoRepository.findById(PERIODO)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L)))
                .isInstanceOf(PeriodoNoEncontradoException.class);
    }

    @Test
    void periodoNoActivo() {
        when(periodoRepository.findById(PERIODO)).thenReturn(Optional.of(periodo("CERRADO")));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L)))
                .isInstanceOf(PeriodoNoActivoException.class);
    }

    @Test
    void fechaFueraDelPeriodo() {
        MatriculaRequest fueraDeRango = new MatriculaRequest(ESTUDIANTE, PERIODO, LocalDate.of(2027, 1, 5), "INICIADA", null);
        assertThatThrownBy(() -> service.registrar(fueraDeRango, detalles(1001L)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("fuera del periodo");
    }

    @Test
    void estudianteInexistente() {
        when(estudiantePort.consultarEstudiante(ESTUDIANTE))
                .thenReturn(EstudianteConsultaPort.EstudianteInfo.inexistente(ESTUDIANTE));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L)))
                .isInstanceOf(EstudianteNoEncontradoException.class);
    }

    @Test
    void estudianteNoHabilitado() {
        when(estudiantePort.consultarEstudiante(ESTUDIANTE))
                .thenReturn(new EstudianteConsultaPort.EstudianteInfo(ESTUDIANTE, true, false, 1L));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L)))
                .isInstanceOf(EstudianteNoHabilitadoException.class);
    }

    @Test
    @DisplayName("Una segunda matricula activa del mismo estudiante en el periodo se rechaza")
    void matriculaDuplicada() {
        when(matriculaRepository.existsByClaveActiva("1001-4")).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L)))
                .isInstanceOf(MatriculaDuplicadaException.class);
        verify(matriculaRepository, never()).saveAndFlush(any());
    }

    @Test
    void seccionRepetida() {
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L, 1001L)))
                .isInstanceOf(SeccionDuplicadaException.class);
    }

    @Test
    void seccionInexistente() {
        assertThatThrownBy(() -> service.registrar(request(), detalles(5555L)))
                .isInstanceOf(SeccionNoEncontradaException.class);
    }

    @Test
    void seccionDeOtroPeriodo() {
        seccion(1007L, 1L, "IS101", 4, 24L, 10, true, horario("LUNES", 8, 10));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1007L)))
                .isInstanceOf(SeccionNoDisponibleException.class).hasMessageContaining("no pertenece");
    }

    @Test
    void seccionCerrada() {
        seccion(1008L, 8L, "IS108", 3, PERIODO, 10, false, horario("SABADO", 8, 10));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1008L)))
                .isInstanceOf(SeccionNoDisponibleException.class).hasMessageContaining("no está abierta");
    }

    @Test
    void sinCupos() {
        seccion(1003L, 3L, "IS103", 3, PERIODO, 0, true, horario("LUNES", 10, 12));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1003L)))
                .isInstanceOf(CupoInsuficienteException.class);
    }

    @Test
    @DisplayName("Dos secciones del mismo curso en una matricula se rechazan")
    void cursoRepetido() {
        seccion(1009L, 1L, "IS101", 4, PERIODO, 10, true, horario("SABADO", 8, 10));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L, 1009L)))
                .isInstanceOf(CursoDuplicadoException.class);
    }

    @Test
    void cruceDeHorario() {
        seccion(1004L, 4L, "IS104", 3, PERIODO, 10, true, horario("LUNES", 9, 11));
        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L, 1004L)))
                .isInstanceOf(CruceHorarioException.class);
    }

    @Test
    @DisplayName("Politica APROBADO: sin el prerrequisito aprobado en un periodo anterior, se rechaza")
    void prerrequisitoNoAprobado() {
        conPrerrequisitoIS101();
        when(matriculaRepository.cursosAcreditadosAntesDe(eq(ESTUDIANTE), any(), any(), eq(ResultadoCurso.APROBADO), eq(0)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.registrar(request(), detalles(1005L)))
                .isInstanceOf(PrerrequisitoNoCumplidoException.class)
                .hasMessageContaining("haber aprobado antes IS101");
    }

    @Test
    @DisplayName("Politica APROBADO: con el prerrequisito aprobado, se registra")
    void prerrequisitoAprobado() {
        conPrerrequisitoIS101();
        when(matriculaRepository.cursosAcreditadosAntesDe(eq(ESTUDIANTE), any(), any(), eq(ResultadoCurso.APROBADO), eq(0)))
                .thenReturn(List.of(1L));

        assertThat(service.registrar(request(), detalles(1005L)).totalCreditos()).isEqualTo(4);
    }

    @Test
    @DisplayName("Politica CURSADO_TEMPORAL: acepta cursos sin resultado y el mensaje la identifica")
    void politicaTemporalIdentificada() throws Exception {
        politica(PoliticaPrerrequisitos.CURSADO_TEMPORAL);
        conPrerrequisitoIS101();
        when(matriculaRepository.cursosAcreditadosAntesDe(eq(ESTUDIANTE), any(), any(), eq(ResultadoCurso.APROBADO), eq(1)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.registrar(request(), detalles(1005L)))
                .isInstanceOf(PrerrequisitoNoCumplidoException.class)
                .hasMessageContaining("CURSADO_TEMPORAL");
    }

    @Test
    @DisplayName("Politica NINGUNA: no consulta prerrequisitos")
    void politicaNinguna() throws Exception {
        politica(PoliticaPrerrequisitos.NINGUNA);
        conPrerrequisitoIS101();

        assertThat(service.registrar(request(), detalles(1005L)).id()).isEqualTo(99L);
        verify(prerrequisitoPort, never()).prerrequisitosDe(anyLong(), any());
    }

    @Test
    @DisplayName("Resultado: solo se registra con el periodo CERRADO")
    void resultadoConPeriodoAbiertoSeRechaza() {
        when(matriculaRepository.findDetalladaById(50L)).thenReturn(Optional.of(matriculaConDetalle(50L, 500L, 1L)));

        assertThatThrownBy(() -> service.registrarResultado(50L, 500L, ResultadoCurso.APROBADO))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("periodo CERRADO");
    }

    @Test
    @DisplayName("Resultado: un detalle previo a la integracion (sin curso) no admite resultado")
    void resultadoEnDetalleLegadoSeRechaza() {
        when(periodoRepository.findById(PERIODO)).thenReturn(Optional.of(periodo("CERRADO")));
        when(matriculaRepository.findDetalladaById(1L)).thenReturn(Optional.of(matriculaConDetalle(1L, 1L, null)));

        assertThatThrownBy(() -> service.registrarResultado(1L, 1L, ResultadoCurso.APROBADO))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no identifica su curso");
    }

    @Test
    void resultadoRegistradoConPeriodoCerrado() {
        when(periodoRepository.findById(PERIODO)).thenReturn(Optional.of(periodo("CERRADO")));
        Matricula matricula = matriculaConDetalle(50L, 500L, 1L);
        when(matriculaRepository.findDetalladaById(50L)).thenReturn(Optional.of(matricula));
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> inv.getArgument(0));

        MatriculaResponse respuesta = service.registrarResultado(50L, 500L, ResultadoCurso.APROBADO);

        assertThat(respuesta.detalles()).singleElement()
                .extracting(d -> d.resultado()).isEqualTo(ResultadoCurso.APROBADO);
        assertThat(matricula.getDetalles().getFirst().getFechaResultado()).isNotNull();
    }

    @Test
    void resultadoDeDetalleAjenoEsNotFound() {
        when(matriculaRepository.findDetalladaById(50L)).thenReturn(Optional.of(matriculaConDetalle(50L, 500L, 1L)));
        assertThatThrownBy(() -> service.registrarResultado(50L, 999L, ResultadoCurso.APROBADO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Si la reserva atomica falla (cupo tomado por otra matricula), la excepcion se propaga: rollback")
    void reservaFallidaPropagaExcepcion() {
        doThrow(new ReglaNegocioException("La seccion 1002 no tiene cupos disponibles"))
                .when(seccionPort).reservarCupo(1002L);

        assertThatThrownBy(() -> service.registrar(request(), detalles(1001L, 1002L)))
                .isInstanceOf(ReglaNegocioException.class);
        verify(cargoPort, never()).generarCargo(anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt());
    }

    /* ------------------------------------------------------------------ */

    private void conPrerrequisitoIS101() {
        seccion(1005L, 5L, "IS201", 4, PERIODO, 10, true, horario("VIERNES", 8, 11));
        when(prerrequisitoPort.prerrequisitosDe(5L, 1L))
                .thenReturn(List.of(new PrerrequisitoConsultaPort.PrerrequisitoInfo(1L, "IS101")));
    }

    private void politica(PoliticaPrerrequisitos politica) throws Exception {
        Field campo = MatriculaService.class.getDeclaredField("politicaPrerrequisitos");
        campo.setAccessible(true);
        campo.set(service, politica);
    }

    private static Matricula matriculaConDetalle(Long matriculaId, Long detalleId, Long cursoId) {
        Matricula m = new Matricula();
        m.setId(matriculaId);
        m.setEstudianteId(ESTUDIANTE);
        m.setPeriodoId(PERIODO);
        m.setFecha(FECHA);
        m.setEstado("INICIADA");
        DetalleMatricula d = new DetalleMatricula();
        d.setId(detalleId);
        d.setSeccionId(1001L);
        d.setCursoId(cursoId);
        d.setMatricula(m);
        m.getDetalles().add(d);
        return m;
    }

    private static MatriculaRequest request() {
        return new MatriculaRequest(ESTUDIANTE, PERIODO, FECHA, "INICIADA", null);
    }

    private static List<DetalleMatriculaRequest> detalles(Long... secciones) {
        return java.util.Arrays.stream(secciones).map(DetalleMatriculaRequest::new).toList();
    }

    private static Periodo periodo(String estado) {
        Periodo p = new Periodo();
        p.setId(PERIODO);
        p.setCodigo("2026-II");
        p.setNombre("Periodo 2026-II");
        p.setFechaInicio(LocalDate.of(2026, 8, 1));
        p.setFechaFin(LocalDate.of(2026, 12, 20));
        p.setEstado(estado);
        return p;
    }

    private static SeccionConsultaPort.HorarioInfo horario(String dia, int desde, int hasta) {
        return new SeccionConsultaPort.HorarioInfo(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private void seccion(Long id, Long cursoId, String codigo, int creditos, Long periodoId, int disponibles,
                         boolean abierta, SeccionConsultaPort.HorarioInfo horario) {
        when(seccionPort.consultarSeccion(id)).thenReturn(new SeccionConsultaPort.SeccionInfo(
                id, 30, disponibles, cursoId, codigo, creditos, periodoId, abierta));
        when(seccionPort.consultarHorarios(id)).thenReturn(List.of(horario));
    }
}
