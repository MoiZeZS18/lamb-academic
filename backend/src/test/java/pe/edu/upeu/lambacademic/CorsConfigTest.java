package pe.edu.upeu.lambacademic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.lambacademic.coreacademico.curso.controller.CursoController;
import pe.edu.upeu.lambacademic.coreacademico.curso.service.CursoService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS configurado por propiedad (S5 3.7): la prueba fija dos origenes en la propiedad
 * (con un espacio despues de la coma, que CorsConfig recorta) sin tocar el codigo.
 * Desde U2 el preflight lo resuelve Spring Security (S11 3.1b): sin token y sin 401.
 */
@WebMvcTest(CursoController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = "lamb.cors.allowed-origins=http://localhost:4200, http://localhost:5173")
class CorsConfigTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CursoService cursoService;

    @Test
    void preflightDesdeOrigenPermitidoDevuelveAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/cursos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void preflightDesdeOrigenNoPermitidoEsRechazado() throws Exception {
        mockMvc.perform(options("/api/v1/cursos")
                        .header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
