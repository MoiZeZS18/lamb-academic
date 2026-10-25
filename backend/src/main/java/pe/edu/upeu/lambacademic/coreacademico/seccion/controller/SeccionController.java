package pe.edu.upeu.lambacademic.coreacademico.seccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.CambioEstadoSeccionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionActualizacionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionOcupacionReporte;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionResponse;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;

import java.util.List;

@Tag(name = "Secciones", description = "Oferta y programacion: secciones, horarios y aulas (Core Academico)")
@RestController
@RequestMapping("/api/v1/secciones")
@RequiredArgsConstructor
public class SeccionController {

    private final SeccionService service;

    @Operation(summary = "Programa una seccion con sus horarios (valida periodo, docente y cruces de aula)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeccionResponse programar(@Valid @RequestBody SeccionRequest request) {
        return service.programar(request);
    }

    @Operation(summary = "Busca secciones con filtros combinados (periodoId, cursoId, docenteId, estado, conVacantes)")
    @GetMapping
    public List<SeccionResponse> buscar(@RequestParam(required = false) Long periodoId,
                                        @RequestParam(required = false) Long cursoId,
                                        @RequestParam(required = false) Long docenteId,
                                        @RequestParam(required = false) EstadoSeccion estado,
                                        @RequestParam(defaultValue = "false") boolean conVacantes,
                                        @RequestParam(defaultValue = "grupo") String sortBy,
                                        @RequestParam(defaultValue = "asc") String dir) {
        return service.buscar(periodoId, cursoId, docenteId, estado, conVacantes, sortBy, dir);
    }

    @Operation(summary = "Consulta una seccion con sus horarios")
    @GetMapping("/{id}")
    public SeccionResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Actualiza docente y limite de vacantes de una seccion")
    @PutMapping("/{id}")
    public SeccionResponse actualizar(@PathVariable Long id, @Valid @RequestBody SeccionActualizacionRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Cambia el estado de la seccion (ABIERTA, CERRADA, CANCELADA)")
    @PatchMapping("/{id}/estado")
    public SeccionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoSeccionRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @Operation(summary = "Elimina una seccion sin matriculas de un periodo no cerrado (si ya se uso, se cancela)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    @Operation(summary = "Reporte agregado: ocupacion de vacantes por curso en un periodo")
    @GetMapping("/resumen")
    public List<SeccionOcupacionReporte> resumen(@RequestParam Long periodoId) {
        return service.ocupacionPorCurso(periodoId);
    }
}
