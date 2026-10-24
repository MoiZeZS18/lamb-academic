package pe.edu.upeu.lambacademic.coreacademico.curso.controller;

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
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoRequest;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResponse;
import pe.edu.upeu.lambacademic.coreacademico.curso.service.CursoService;

import java.util.List;

@Tag(name = "Cursos", description = "Catalogo de cursos (Core Academico)")
@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService service;

    @Operation(summary = "Busca cursos con filtros combinados (activo, creditos, q) y orden")
    @GetMapping
    public List<CursoResponse> buscar(@RequestParam(required = false) Boolean activo,
                                      @RequestParam(required = false) Integer creditos,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "codigo") String sortBy,
                                      @RequestParam(defaultValue = "asc") String dir) {
        return service.buscar(activo, creditos, q, sortBy, dir);
    }

    @Operation(summary = "Consulta un curso por id")
    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Registra un curso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse crear(@Valid @RequestBody CursoRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Actualiza un curso")
    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Desactiva un curso (borrado logico)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
