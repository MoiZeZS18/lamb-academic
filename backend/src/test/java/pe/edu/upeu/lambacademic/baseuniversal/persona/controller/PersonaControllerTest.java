package pe.edu.upeu.lambacademic.baseuniversal.persona.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.lambacademic.CorsConfig;
import pe.edu.upeu.lambacademic.SecurityConfig;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static pe.edu.upeu.lambacademic.TokensPrueba.admin;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonaController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class PersonaControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PersonaService personaService;

    @Test
    void crearValidaRespondeCreated() throws Exception {
        when(personaService.crear(any())).thenReturn(new PersonaResponse(1L, Persona.TipoPersona.ALUMNO,
                Persona.TipoDocumento.DNI, "70000009", "Ana", "Quispe", null, "Quispe Ana", null, null, null,
                null, null, null, true));

        mockMvc.perform(post("/api/v1/personas").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipoPersona":"ALUMNO","tipoDocumento":"DNI","numeroDocumento":"70000009",
                         "nombres":"Ana","apellidoPaterno":"Quispe"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreCompleto").value("Quispe Ana"));
    }

    @Test
    void documentoConLetrasRespondeBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/personas").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipoPersona":"ALUMNO","tipoDocumento":"DNI","numeroDocumento":"ABC",
                         "nombres":"Ana","apellidoPaterno":"Quispe","email":"no-es-correo"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numeroDocumento").exists())
                .andExpect(jsonPath("$.errores.email").exists());
        verifyNoInteractions(personaService);
    }

    @Test
    void documentoDuplicadoRespondeConflict() throws Exception {
        when(personaService.crear(any())).thenThrow(new ReglaNegocioException("Ya existe una persona con DNI 70000001"));

        mockMvc.perform(post("/api/v1/personas").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipoPersona":"ALUMNO","tipoDocumento":"DNI","numeroDocumento":"70000001",
                         "nombres":"Ana","apellidoPaterno":"Quispe"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void personaInexistenteRespondeNotFound() throws Exception {
        when(personaService.obtener(999L)).thenThrow(new ResourceNotFoundException("Persona", 999L));

        mockMvc.perform(get("/api/v1/personas/999").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Persona no encontrado: 999"));
    }

    @Test
    void tipoDePersonaInvalidoEnFiltroRespondeBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/personas").with(admin()).param("tipoPersona", "MARCIANO"))
                .andExpect(status().isBadRequest());
    }
}
