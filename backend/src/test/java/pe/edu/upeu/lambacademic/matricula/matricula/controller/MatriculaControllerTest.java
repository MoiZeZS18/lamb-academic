package pe.edu.upeu.lambacademic.matricula.matricula.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.lambacademic.CorsConfig;
import pe.edu.upeu.lambacademic.SecurityConfig;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.CargoMatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.DetalleMatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.CupoInsuficienteException;
import pe.edu.upeu.lambacademic.matricula.matricula.exception.MatriculaNoEncontradaException;
import pe.edu.upeu.lambacademic.matricula.matricula.service.MatriculaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static pe.edu.upeu.lambacademic.TokensPrueba.coordinador;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatriculaController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class MatriculaControllerTest {

    private static final String VALIDA = """
            {"matricula":{"estudianteId":1001,"periodoId":4,"fecha":"2026-10-24","estado":"INICIADA"},
             "detalles":[{"seccionId":1001},{"seccionId":1002}]}
            """;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MatriculaService matriculaService;

    @Test
    void registrarValidaRespondeCreatedConCargoYTraceId() throws Exception {
        when(matriculaService.registrar(any(), any())).thenReturn(new MatriculaResponse(99L, 1001L, 4L,
                LocalDate.of(2026, 10, 24), "INICIADA",
                List.of(new DetalleMatriculaResponse(1L, 1001L, 1L, 4, null), new DetalleMatriculaResponse(2L, 1002L, 2L, 4, null)),
                8, new CargoMatriculaResponse(7L, "CRG-1", new BigDecimal("550.00"), 5, "EMITIDO")));

        mockMvc.perform(post("/api/v1/matriculas").with(coordinador()).contentType(MediaType.APPLICATION_JSON).content(VALIDA)
                        .header("X-Trace-ID", "traza-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-ID", "traza-123"))
                .andExpect(jsonPath("$.totalCreditos").value(8))
                .andExpect(jsonPath("$.cargo.montoTotal").value(550.00))
                .andExpect(jsonPath("$.detalles.length()").value(2));
    }

    @Test
    void detallesVaciosRespondeBadRequestSinLlegarAlService() throws Exception {
        String sinDetalles = """
                {"matricula":{"estudianteId":1001,"periodoId":4,"fecha":"2026-10-24","estado":"INICIADA"},
                 "detalles":[]}
                """;
        mockMvc.perform(post("/api/v1/matriculas").with(coordinador()).contentType(MediaType.APPLICATION_JSON).content(sinDetalles))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.detalles").exists())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        verifyNoInteractions(matriculaService);
    }

    @Test
    void cuposAgotadosRespondeConflict() throws Exception {
        when(matriculaService.registrar(any(), any()))
                .thenThrow(new CupoInsuficienteException("La sección 1003 no tiene cupos disponibles"));

        mockMvc.perform(post("/api/v1/matriculas").with(coordinador()).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("La sección 1003 no tiene cupos disponibles"));
    }

    @Test
    void matriculaInexistenteRespondeNotFound() throws Exception {
        when(matriculaService.obtener(123L, null)).thenThrow(new MatriculaNoEncontradaException("La matricula 123 no existe"));

        mockMvc.perform(get("/api/v1/matriculas/123").with(coordinador()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/matriculas/123"));
    }

    @Test
    void jsonMalFormadoRespondeBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/matriculas").with(coordinador()).contentType(MediaType.APPLICATION_JSON).content("{\"matricula\":"))
                .andExpect(status().isBadRequest());
    }
}
