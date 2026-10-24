package pe.edu.upeu.lambacademic.baseuniversal.persona.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.ConteoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;

import java.util.List;

@Tag(name = "Personas", description = "Maestro de personas de Base Universal")
@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService service;

    @Operation(summary = "Busca personas con filtros combinados (tipoPersona, activo, q, idEscuela) y orden")
    @GetMapping
    public List<PersonaResponse> buscar(@RequestParam(required = false) Persona.TipoPersona tipoPersona,
                                        @RequestParam(required = false) Boolean activo,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) Long idEscuela,
                                        @RequestParam(defaultValue = "apellidoPaterno") String sortBy,
                                        @RequestParam(defaultValue = "asc") String dir) {
        return service.buscar(tipoPersona, activo, q, idEscuela, sortBy, dir);
    }

    @Operation(summary = "Consulta una persona por id")
    @GetMapping("/{id}")
    public PersonaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Consulta una persona por tipo y numero de documento")
    @GetMapping("/documento/{tipoDocumento}/{numero}")
    public PersonaResponse obtenerPorDocumento(@PathVariable Persona.TipoDocumento tipoDocumento,
                                               @PathVariable String numero) {
        return service.obtenerPorDocumento(tipoDocumento, numero);
    }

    @Operation(summary = "Registra una persona")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse crear(@Valid @RequestBody PersonaRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Actualiza una persona")
    @PutMapping("/{id}")
    public PersonaResponse actualizar(@PathVariable Long id, @Valid @RequestBody PersonaRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Desactiva una persona (borrado logico)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }

    @Operation(summary = "Reporte agregado: personas activas por tipo")
    @GetMapping("/resumen")
    public List<ConteoResponse> resumen() {
        return service.resumenPorTipo();
    }

    @Operation(summary = "Reporte agregado: personas activas por escuela")
    @GetMapping("/resumen/escuela")
    public List<ConteoResponse> resumenPorEscuela() {
        return service.resumenPorEscuela();
    }
}
