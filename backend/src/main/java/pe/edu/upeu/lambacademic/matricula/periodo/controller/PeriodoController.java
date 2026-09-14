package pe.edu.upeu.lambacademic.matricula.periodo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoResponse;
import pe.edu.upeu.lambacademic.matricula.periodo.service.PeriodoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/periodos")
@RequiredArgsConstructor
public class PeriodoController {

    private final PeriodoService periodoService;

    @GetMapping
    public List<PeriodoResponse> listar() {
        return periodoService.listar();
    }

    @GetMapping("/{id}")
    public PeriodoResponse obtenerPorId(@PathVariable Long id) {
        return periodoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodoResponse crear(@Valid @RequestBody PeriodoRequest request) {
        return periodoService.crear(request);
    }

    @PutMapping("/{id}")
    public PeriodoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PeriodoRequest request) {

        return periodoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        periodoService.eliminar(id);
    }
}