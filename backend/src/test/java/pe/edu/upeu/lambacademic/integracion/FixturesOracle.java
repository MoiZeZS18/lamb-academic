package pe.edu.upeu.lambacademic.integracion;

import org.springframework.jdbc.core.JdbcTemplate;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.service.PeriodoService;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.AlumnoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.service.AlumnoService;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.FacultadRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.SedeRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.service.InstitucionalService;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoRequest;
import pe.edu.upeu.lambacademic.coreacademico.curso.service.CursoService;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.HorarioAulaRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.DiaSemana;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Crea datos de prueba propios (codigos unicos por ejecucion) a traves de los servicios reales
 * y los elimina al final, solo por los ids que ella misma creo. No toca otros registros.
 */
public class FixturesOracle {

    public final String sufijo = String.format("%05d", ThreadLocalRandom.current().nextInt(100_000));

    private final JdbcTemplate jdbc;
    private final PersonaService personaService;
    private final AlumnoService alumnoService;
    private final CursoService cursoService;
    private final SeccionService seccionService;
    private final InstitucionalService institucionalService;

    private final List<Long> personas = new ArrayList<>();
    private final List<Long> alumnos = new ArrayList<>();
    private final List<Long> cursos = new ArrayList<>();
    private final List<Long> secciones = new ArrayList<>();
    private final List<Long> planes = new ArrayList<>();
    private final List<Long> periodos = new ArrayList<>();
    private Long sedeId;
    private Long facultadId;
    private Long escuelaId;
    private int contador;

    public FixturesOracle(JdbcTemplate jdbc, PersonaService personaService, AlumnoService alumnoService,
                          CursoService cursoService, SeccionService seccionService,
                          InstitucionalService institucionalService) {
        this.jdbc = jdbc;
        this.personaService = personaService;
        this.alumnoService = alumnoService;
        this.cursoService = cursoService;
        this.seccionService = seccionService;
        this.institucionalService = institucionalService;
    }

    /** Periodo ACTIVO real del esquema (2026-II). */
    public Long periodoActivo() {
        return jdbc.queryForObject("SELECT MIN(ID) FROM PERIODOS WHERE ESTADO = 'ACTIVO'", Long.class);
    }

    public Long alumno() {
        contador++;
        String dni = "9" + sufijo + String.format("%02d", contador);
        Long personaId = personaService.crear(new PersonaRequest(Persona.TipoPersona.ALUMNO, Persona.TipoDocumento.DNI,
                dni, "Prueba" + contador, "Integracion", null, null, null, null, null, null)).id();
        personas.add(personaId);
        Long alumnoId = alumnoService.crear(new AlumnoRequest(personaId, "IT" + sufijo + contador,
                LocalDate.of(2025, 3, 1))).id();
        alumnos.add(alumnoId);
        return alumnoId;
    }

    public Long curso(int creditos) {
        contador++;
        Long id = cursoService.crear(new CursoRequest("IT" + sufijo + contador, "Curso de integracion " + contador,
                creditos, 2, 2)).id();
        cursos.add(id);
        return id;
    }

    /** Seccion del sabado en un aula exclusiva de la prueba (sin cruces con datos reales). */
    public Long seccion(Long cursoId, Long periodoId, int vacantes, int horaInicio) {
        contador++;
        Long id = seccionService.programar(new SeccionRequest(cursoId, periodoId, "T" + contador, null, vacantes,
                List.of(new HorarioAulaRequest(DiaSemana.SABADO, LocalTime.of(horaInicio, 0),
                        LocalTime.of(horaInicio + 1, 0), "IT-" + sufijo + "-" + contador)))).id();
        secciones.add(id);
        return id;
    }

    public Long escuela() {
        if (escuelaId == null) {
            sedeId = institucionalService.crearSede(new SedeRequest("ITS" + sufijo, "Sede de prueba", null)).id();
            facultadId = institucionalService.crearFacultad(new FacultadRequest(sedeId, "ITF" + sufijo, "Facultad prueba")).id();
            escuelaId = institucionalService.crearEscuela(new EscuelaRequest(facultadId, "ITE" + sufijo, "Escuela prueba", null)).id();
        }
        return escuelaId;
    }

    /** Periodo pasado propio de la prueba (ACTIVO al crearlo, para poder matricular en el). */
    public Long periodoPasado(PeriodoService periodoService) {
        contador++;
        Long id = periodoService.crear(new PeriodoRequest("IT" + sufijo + contador, "Periodo de integracion",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 7, 15), "ACTIVO")).id();
        periodos.add(id);
        return id;
    }

    public void registrarPlan(Long planId) {
        planes.add(planId);
    }

    public int cuposOcupados(Long seccionId) {
        return jdbc.queryForObject("SELECT CUPOS_OCUPADOS FROM SECCIONES WHERE ID = ?", Integer.class, seccionId);
    }

    public int matriculasDe(Long alumnoId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM MATRICULAS WHERE ESTUDIANTE_ID = ?", Integer.class, alumnoId);
    }

    public int cargosDe(Long alumnoId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM FIN_CARGOS WHERE ID_ESTUDIANTE = ?", Integer.class, alumnoId);
    }

    /** Elimina SOLO lo creado por esta prueba, hijos primero. */
    public void limpiar() {
        for (Long alumno : alumnos) {
            jdbc.update("DELETE FROM FIN_PAGOS WHERE ID_CARGO IN (SELECT ID FROM FIN_CARGOS WHERE ID_ESTUDIANTE = ?)", alumno);
            jdbc.update("DELETE FROM FIN_DETALLE_CARGOS WHERE ID_CARGO IN (SELECT ID FROM FIN_CARGOS WHERE ID_ESTUDIANTE = ?)", alumno);
            jdbc.update("DELETE FROM FIN_CARGOS WHERE ID_ESTUDIANTE = ?", alumno);
            jdbc.update("DELETE FROM DETALLE_MATRICULA WHERE MATRICULA_ID IN (SELECT ID FROM MATRICULAS WHERE ESTUDIANTE_ID = ?)", alumno);
            jdbc.update("DELETE FROM MATRICULAS WHERE ESTUDIANTE_ID = ?", alumno);
        }
        for (Long plan : planes) {
            jdbc.update("DELETE FROM DETALLE_PLAN_ESTUDIO WHERE ID_PLAN = ?", plan);
            jdbc.update("DELETE FROM PLAN_ESTUDIO WHERE ID = ?", plan);
        }
        for (Long seccion : secciones) {
            jdbc.update("DELETE FROM HORARIOS_AULA WHERE ID_SECCION = ?", seccion);
            jdbc.update("DELETE FROM SECCIONES WHERE ID = ?", seccion);
        }
        periodos.forEach(id -> jdbc.update("DELETE FROM PERIODOS WHERE ID = ?", id));
        alumnos.forEach(id -> jdbc.update("DELETE FROM ALUMNOS WHERE ID = ?", id));
        personas.forEach(id -> jdbc.update("DELETE FROM PERSONA WHERE ID = ?", id));
        cursos.forEach(id -> jdbc.update("DELETE FROM CURSO WHERE ID = ?", id));
        if (escuelaId != null) {
            jdbc.update("DELETE FROM ESCUELA WHERE ID = ?", escuelaId);
            jdbc.update("DELETE FROM FACULTAD WHERE ID = ?", facultadId);
            jdbc.update("DELETE FROM SEDE WHERE ID = ?", sedeId);
        }
    }
}
