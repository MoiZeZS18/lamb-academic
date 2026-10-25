package pe.edu.upeu.lambacademic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.lambacademic.coreacademico.seccion.controller.SeccionController;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;
import pe.edu.upeu.lambacademic.finanzas.cargo.controller.CargoController;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.service.CargoService;
import pe.edu.upeu.lambacademic.matricula.matricula.controller.MatriculaController;
import pe.edu.upeu.lambacademic.matricula.matricula.service.MatriculaService;
import pe.edu.upeu.lambacademic.matricula.periodo.controller.PeriodoController;
import pe.edu.upeu.lambacademic.matricula.periodo.service.PeriodoService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.edu.upeu.lambacademic.TokensPrueba.admin;
import static pe.edu.upeu.lambacademic.TokensPrueba.cajero;
import static pe.edu.upeu.lambacademic.TokensPrueba.coordinador;
import static pe.edu.upeu.lambacademic.TokensPrueba.estudiante;
import static pe.edu.upeu.lambacademic.TokensPrueba.estudianteSinAlumno;

/**
 * Matriz de accesos del backend (S10 3.16 / S10b 8, docs/SEGURIDAD_KEYCLOAK.md): sin token 401,
 * rol insuficiente 403, rol correcto 2xx, y el filtro por dueno de ESTUDIANTE (ABAC).
 */
@WebMvcTest({PeriodoController.class, SeccionController.class, MatriculaController.class,
        CargoController.class, PerfilController.class})
@Import({SecurityConfig.class, CorsConfig.class})
class SecurityConfigTest {

    private static final String PERIODO = """
            {"codigo":"2027-I","nombre":"Periodo 2027-I","fechaInicio":"2027-03-01",
             "fechaFin":"2027-07-15","estado":"PLANIFICADO"}
            """;
    private static final String MATRICULA_DE_1002 = """
            {"matricula":{"estudianteId":1002,"periodoId":4,"fecha":"2026-10-24","estado":"INICIADA"},
             "detalles":[{"seccionId":1004}]}
            """;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PeriodoService periodoService;
    @MockitoBean private SeccionService seccionService;
    @MockitoBean private MatriculaService matriculaService;
    @MockitoBean private CargoService cargoService;

    @Test
    void sinTokenResponde401ConCuerpoComunYCabeceraEstandar() throws Exception {
        mockMvc.perform(get("/api/v1/periodos").header("X-Trace-ID", "traza-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(header().string("X-Trace-ID", "traza-401"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").value("traza-401"));
        verifyNoInteractions(periodoService);
    }

    @Test
    void consultarPeriodosBastaConSesion() throws Exception {
        when(periodoService.listar()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/periodos").with(estudiante(1001))).andExpect(status().isOk());
    }

    @Test
    void crearPeriodoExigeAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/periodos").with(estudiante(1001))
                        .contentType(MediaType.APPLICATION_JSON).content(PERIODO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Tu rol no tiene permiso para esta operación"));
        mockMvc.perform(post("/api/v1/periodos").with(coordinador())
                        .contentType(MediaType.APPLICATION_JSON).content(PERIODO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(periodoService);

        mockMvc.perform(post("/api/v1/periodos").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(PERIODO))
                .andExpect(status().isCreated());
    }

    @Test
    void eliminarSeccionExigeCoordinacion() throws Exception {
        mockMvc.perform(delete("/api/v1/secciones/1004").with(estudiante(1001))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/secciones/1004").with(cajero())).andExpect(status().isForbidden());
        verifyNoInteractions(seccionService);
        mockMvc.perform(delete("/api/v1/secciones/1004").with(coordinador())).andExpect(status().isNoContent());
        verify(seccionService).eliminar(1004L);
    }

    @Test
    void estudianteNoRegistraMatriculaDeOtroAlumno() throws Exception {
        mockMvc.perform(post("/api/v1/matriculas").with(estudiante(1001))
                        .contentType(MediaType.APPLICATION_JSON).content(MATRICULA_DE_1002))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes acceso a la matrícula de otro estudiante"));
        verifyNoInteractions(matriculaService);
    }

    @Test
    void coordinadorRegistraMatriculaDeCualquierAlumno() throws Exception {
        mockMvc.perform(post("/api/v1/matriculas").with(coordinador())
                        .contentType(MediaType.APPLICATION_JSON).content(MATRICULA_DE_1002))
                .andExpect(status().isCreated());
        verify(matriculaService).registrar(any(), any());
    }

    @Test
    void listadoDeEstudianteSeLimitaASusMatriculas() throws Exception {
        when(matriculaService.buscar(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Page.empty());
        mockMvc.perform(get("/api/v1/matriculas").with(estudiante(1001))).andExpect(status().isOk());
        verify(matriculaService).buscar(eq(1001L), isNull(), isNull(), eq(0), eq(10), eq("fecha"), eq("desc"));

        mockMvc.perform(get("/api/v1/matriculas").param("estudianteId", "1002").with(estudiante(1001)))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultaPorIdPasaElAlumnoDelTokenAlServicio() throws Exception {
        mockMvc.perform(get("/api/v1/matriculas/93").with(estudiante(1033))).andExpect(status().isOk());
        verify(matriculaService).obtener(93L, 1033L);
        mockMvc.perform(get("/api/v1/matriculas/93").with(coordinador())).andExpect(status().isOk());
        verify(matriculaService).obtener(93L, null);
    }

    @Test
    void estudianteSinAlumnoVinculadoRecibe403() throws Exception {
        mockMvc.perform(get("/api/v1/matriculas/93").with(estudianteSinAlumno()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no está vinculada a ningún alumno")));
        verifyNoInteractions(matriculaService);
    }

    @Test
    void anularYReportesSonDeCoordinacion() throws Exception {
        mockMvc.perform(patch("/api/v1/matriculas/93/anular").with(estudiante(1001))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/matriculas/resumen").with(estudiante(1001))).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/matriculas/93/anular").with(coordinador())).andExpect(status().isOk());
        verify(matriculaService).anular(93L);
    }

    @Test
    void cargoAjenoResponde403YPropio200() throws Exception {
        when(cargoService.obtener(7L)).thenReturn(new CargoResponse(7L, "CRG-7", 1001L, null, 93L, 4L, 11, 5,
                null, null, null, null, null, List.of()));
        mockMvc.perform(get("/api/v1/finanzas/cargos/7").with(estudiante(1033))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/finanzas/cargos/7").with(estudiante(1001))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/finanzas/cargos").with(estudiante(1001))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/finanzas/cargos/7/pagos").with(estudiante(1001))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":10,\"medioPago\":\"EFECTIVO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilSeLeeDelTokenSinRolesInternosDeKeycloak() throws Exception {
        mockMvc.perform(get("/api/v1/auth/perfil").with(estudiante(1001)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.alumnoId").value(1001));
    }

    @Test
    void preflightCorsNoExigeToken() throws Exception {
        mockMvc.perform(options("/api/v1/matriculas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
