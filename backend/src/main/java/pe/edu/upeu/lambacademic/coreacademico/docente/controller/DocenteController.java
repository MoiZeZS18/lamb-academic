package pe.edu.upeu.lambacademic.coreacademico.docente.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.coreacademico.docente.dto.CargaDocenteResponse;
import pe.edu.upeu.lambacademic.coreacademico.docente.service.DocenteService;

import java.util.List;

@Tag(name = "Docentes", description = "Plana docente y carga lectiva (Core Academico - Curriculo)")
@RestController
@RequestMapping("/api/v1/docentes")
@RequiredArgsConstructor
public class DocenteController {

    private final DocenteService service;

    @Operation(summary = "Lista la plana docente (personas de tipo DOCENTE)")
    @GetMapping
    public List<PersonaResponse> listar(@RequestParam(required = false) Boolean activo,
                                        @RequestParam(required = false) String q) {
        return service.listar(activo, q);
    }

    @Operation(summary = "Carga lectiva del docente: secciones y horas semanales en un periodo")
    @GetMapping("/{id}/carga")
    public CargaDocenteResponse carga(@PathVariable Long id, @RequestParam Long periodoId) {
        return service.carga(id, periodoId);
    }
}
