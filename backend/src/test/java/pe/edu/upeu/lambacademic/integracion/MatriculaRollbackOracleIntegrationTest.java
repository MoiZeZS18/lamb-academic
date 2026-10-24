package pe.edu.upeu.lambacademic.integracion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.service.AlumnoService;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.service.InstitucionalService;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.coreacademico.curso.service.CursoService;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.port.CargoFinancieroPort;
import pe.edu.upeu.lambacademic.matricula.matricula.service.MatriculaService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ROLLBACK PROVOCADO a proposito (S6, secuencia de demo, paso 3): Finanzas falla DESPUES de
 * que la matricula ya hizo INSERT de cabecera y detalles y UPDATE de cupos en Oracle.
 * Como todo ocurre en una sola transaccion (@Transactional en MatriculaService.registrar,
 * propagacion REQUIRED en los servicios llamados), Oracle revierte todo.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatriculaRollbackOracleIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PersonaService personaService;
    @Autowired private AlumnoService alumnoService;
    @Autowired private CursoService cursoService;
    @Autowired private SeccionService seccionService;
    @Autowired private InstitucionalService institucionalService;
    @Autowired private MatriculaService matriculaService;

    /** Reemplaza al adaptador real de Finanzas solo en esta prueba para provocar el fallo. */
    @MockitoBean private CargoFinancieroPort cargoFinancieroPort;

    private FixturesOracle fx;

    @BeforeAll
    void crearDatos() {
        fx = new FixturesOracle(jdbc, personaService, alumnoService, cursoService, seccionService, institucionalService);
    }

    @AfterAll
    void limpiarDatos() {
        fx.limpiar();
    }

    @Test
    @DisplayName("Falla Finanzas al final: no queda matricula, ni detalles, ni cupos reservados")
    void falloEnFinanzasRevierteTodo() {
        Long periodo = fx.periodoActivo();
        Long alumno = fx.alumno();
        Long seccion1 = fx.seccion(fx.curso(4), periodo, 10, 15);
        Long seccion2 = fx.seccion(fx.curso(3), periodo, 10, 17);
        when(cargoFinancieroPort.generarCargo(anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("Fallo simulado en Finanzas"));
        Integer detallesAntes = jdbc.queryForObject("SELECT COUNT(*) FROM DETALLE_MATRICULA", Integer.class);

        assertThatThrownBy(() -> matriculaService.registrar(
                new MatriculaRequest(alumno, periodo, LocalDate.now(), "INICIADA", 3),
                List.of(new DetalleMatriculaRequest(seccion1), new DetalleMatriculaRequest(seccion2))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fallo simulado en Finanzas");

        assertThat(fx.matriculasDe(alumno)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DETALLE_MATRICULA", Integer.class)).isEqualTo(detallesAntes);
        assertThat(fx.cuposOcupados(seccion1)).isZero();
        assertThat(fx.cuposOcupados(seccion2)).isZero();
    }

    @Test
    @DisplayName("Control: con Finanzas funcionando, la misma operacion si se confirma")
    void sinFalloSeConfirma() {
        Long periodo = fx.periodoActivo();
        Long alumno = fx.alumno();
        Long seccion = fx.seccion(fx.curso(2), periodo, 10, 19);
        when(cargoFinancieroPort.generarCargo(anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(new CargoFinancieroPort.CargoInfo(0L, "CRG-SIMULADO", java.math.BigDecimal.TEN, 1, "EMITIDO"));

        matriculaService.registrar(new MatriculaRequest(alumno, periodo, LocalDate.now(), "INICIADA", 1),
                List.of(new DetalleMatriculaRequest(seccion)));

        assertThat(fx.matriculasDe(alumno)).isEqualTo(1);
        assertThat(fx.cuposOcupados(seccion)).isEqualTo(1);
    }
}
