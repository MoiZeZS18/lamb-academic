package pe.edu.upeu.lambacademic.finanzas.cargo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoEstadoReporte;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoRequest;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResumenResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.PagoRequest;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.PagoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.SaldoEstudianteResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;
import pe.edu.upeu.lambacademic.finanzas.cargo.service.CargoService;

import java.util.List;

@Tag(name = "Finanzas del Estudiante", description = "Cargos, cuotas, pagos y saldos")
@RestController
@RequestMapping("/api/v1/finanzas/cargos")
@RequiredArgsConstructor
public class CargoController {

    private final CargoService service;

    @Operation(summary = "Registra un cargo manual con su fraccionamiento en cuotas")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CargoResponse registrar(@Valid @RequestBody CargoRequest request) {
        return service.registrarCargo(request);
    }

    @Operation(summary = "Busca cargos con filtros combinados (estudianteId, periodoId, estado, procesoOrigen)")
    @GetMapping
    public List<CargoResumenResponse> buscar(@RequestParam(required = false) Long estudianteId,
                                             @RequestParam(required = false) Long periodoId,
                                             @RequestParam(required = false) EstadoCargo estado,
                                             @RequestParam(required = false) ProcesoOrigen procesoOrigen,
                                             @RequestParam(defaultValue = "fechaEmision") String sortBy,
                                             @RequestParam(defaultValue = "desc") String dir) {
        return service.buscar(estudianteId, periodoId, estado, procesoOrigen, sortBy, dir);
    }

    /* [U2] Un ESTUDIANTE solo consulta sus propios cargos (claim alumno_id); el personal, todos. */

    @Operation(summary = "Consulta un cargo con el detalle de cuotas")
    @GetMapping("/{id}")
    public CargoResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        CargoResponse cargo = service.obtener(id);
        IdentidadToken.exigirPropio(IdentidadToken.estudianteRestringido(jwt), cargo.estudianteId(), "los datos del cargo");
        return cargo;
    }

    @Operation(summary = "Lista los cargos de un estudiante con sus cuotas")
    @GetMapping("/estudiante/{estudianteId}")
    public List<CargoResponse> listarDeEstudiante(@PathVariable Long estudianteId, @AuthenticationPrincipal Jwt jwt) {
        IdentidadToken.exigirPropio(IdentidadToken.estudianteRestringido(jwt), estudianteId, "los cargos");
        return service.listarDeEstudiante(estudianteId);
    }

    @Operation(summary = "Saldo consolidado del estudiante")
    @GetMapping("/estudiante/{estudianteId}/saldo")
    public SaldoEstudianteResponse saldo(@PathVariable Long estudianteId, @AuthenticationPrincipal Jwt jwt) {
        IdentidadToken.exigirPropio(IdentidadToken.estudianteRestringido(jwt), estudianteId, "la cuenta");
        return service.saldoDeEstudiante(estudianteId);
    }

    @Operation(summary = "Anula un cargo sin pagos")
    @PatchMapping("/{id}/anular")
    public CargoResponse anular(@PathVariable Long id) {
        return service.anular(id);
    }

    @Operation(summary = "Registra un pago y lo aplica a las cuotas en orden")
    @PostMapping("/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public CargoResponse registrarPago(@PathVariable Long id, @Valid @RequestBody PagoRequest request) {
        return service.registrarPago(id, request);
    }

    @Operation(summary = "Lista los pagos de un cargo")
    @GetMapping("/{id}/pagos")
    public List<PagoResponse> listarPagos(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long propio = IdentidadToken.estudianteRestringido(jwt);
        if (propio != null) {
            IdentidadToken.exigirPropio(propio, service.obtener(id).estudianteId(), "los pagos");
        }
        return service.listarPagos(id);
    }

    @Operation(summary = "Reporte agregado: cargos, montos y saldos por estado (opcionalmente por periodo)")
    @GetMapping("/resumen")
    public List<CargoEstadoReporte> resumen(@RequestParam(required = false) Long periodoId) {
        return service.resumen(periodoId);
    }
}
