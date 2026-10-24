package pe.edu.upeu.lambacademic.matricula.periodo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoResponse;
import pe.edu.upeu.lambacademic.matricula.periodo.service.PeriodoService;

import java.util.List;

@Tag(name = "Periodos", description = "Periodos academicos (modulo Matricula)")
@RestController
@RequestMapping("/api/v1/periodos")
@RequiredArgsConstructor
public class PeriodoController {

    private final PeriodoService periodoService;

    @Operation(summary = "Lista los periodos academicos")
    @GetMapping
    public List<PeriodoResponse> listar() {
        return periodoService.listar();
    }

    @Operation(summary = "Consulta un periodo por id")
    @GetMapping("/{id}")
    public PeriodoResponse obtenerPorId(@PathVariable Long id) {
        return periodoService.obtenerPorId(id);
    }

    @Operation(summary = "Registra un periodo (estado PLANIFICADO, ACTIVO o CERRADO)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodoResponse crear(@Valid @RequestBody PeriodoRequest request) {
        return periodoService.crear(request);
    }

    @Operation(summary = "Actualiza un periodo")
    @PutMapping("/{id}")
    public PeriodoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PeriodoRequest request) {

        return periodoService.actualizar(id, request);
    }

    @Operation(summary = "Elimina un periodo sin matriculas ni secciones")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        periodoService.eliminar(id);
    }
}