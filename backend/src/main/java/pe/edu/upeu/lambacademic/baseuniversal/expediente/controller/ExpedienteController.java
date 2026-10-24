package pe.edu.upeu.lambacademic.baseuniversal.expediente.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.CambioEstadoExpedienteRequest;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.DocumentoAdjuntoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.DocumentoAdjuntoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteEstadoReporte;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteRequest;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteResponse;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.dto.ExpedienteResumen;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.EstadoExpediente;
import pe.edu.upeu.lambacademic.baseuniversal.expediente.service.ExpedienteService;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Expedientes", description = "Cabecera-detalle Expediente - DocumentoAdjunto (Base Universal)")
@RestController
@RequestMapping("/api/v1/expedientes")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteService service;

    @Operation(summary = "Registra un expediente con sus documentos en una sola transaccion")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpedienteResponse registrar(@Valid @RequestBody ExpedienteRequest request) {
        return service.registrar(request);
    }

    @Operation(summary = "Busca expedientes con filtros combinados (personaId, estado, desde, hasta) y orden")
    @GetMapping
    public List<ExpedienteResumen> buscar(
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) EstadoExpediente estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "fechaRegistro") String sortBy,
            @RequestParam(defaultValue = "desc") String dir) {
        return service.buscar(personaId, estado, desde, hasta, sortBy, dir);
    }

    @Operation(summary = "Consulta un expediente con sus documentos")
    @GetMapping("/{id}")
    public ExpedienteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Lista los documentos de un expediente")
    @GetMapping("/{id}/documentos")
    public List<DocumentoAdjuntoResponse> listarDocumentos(@PathVariable Long id) {
        return service.listarDocumentos(id);
    }

    @Operation(summary = "Agrega un documento a un expediente REGISTRADO u OBSERVADO")
    @PostMapping("/{id}/documentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpedienteResponse agregarDocumento(@PathVariable Long id,
                                               @Valid @RequestBody DocumentoAdjuntoRequest request) {
        return service.agregarDocumento(id, request);
    }

    @Operation(summary = "Cambia el estado del expediente segun el flujo permitido")
    @PatchMapping("/{id}/estado")
    public ExpedienteResponse cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody CambioEstadoExpedienteRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @Operation(summary = "Reporte agregado: expedientes y documentos por estado")
    @GetMapping("/resumen")
    public List<ExpedienteEstadoReporte> resumen() {
        return service.resumen();
    }
}
