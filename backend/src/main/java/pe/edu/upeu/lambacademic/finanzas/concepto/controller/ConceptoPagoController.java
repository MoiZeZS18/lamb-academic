package pe.edu.upeu.lambacademic.finanzas.concepto.controller;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.finanzas.concepto.dto.ConceptoPagoRequest;
import pe.edu.upeu.lambacademic.finanzas.concepto.dto.ConceptoPagoResponse;
import pe.edu.upeu.lambacademic.finanzas.concepto.service.ConceptoPagoService;

import java.util.List;

@Tag(name = "Conceptos de pago", description = "Tarifas de Finanzas del Estudiante")
@RestController
@RequestMapping("/api/v1/finanzas/conceptos")
@RequiredArgsConstructor
public class ConceptoPagoController {

    private final ConceptoPagoService service;

    @Operation(summary = "Lista los conceptos de pago")
    @GetMapping
    public List<ConceptoPagoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Consulta un concepto de pago")
    @GetMapping("/{id}")
    public ConceptoPagoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Registra un concepto de pago")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConceptoPagoResponse crear(@Valid @RequestBody ConceptoPagoRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Actualiza un concepto de pago (solo afecta cargos futuros)")
    @PutMapping("/{id}")
    public ConceptoPagoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ConceptoPagoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Desactiva un concepto de pago")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
