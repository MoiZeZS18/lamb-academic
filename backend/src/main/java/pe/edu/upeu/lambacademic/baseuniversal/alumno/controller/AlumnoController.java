package pe.edu.upeu.lambacademic.baseuniversal.alumno.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.AlumnoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.AlumnoResponse;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.dto.CambioEstadoAlumnoRequest;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.EstadoAlumno;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.service.AlumnoService;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.ConteoResponse;

import java.util.List;

@Tag(name = "Alumnos", description = "Estudiantes de Base Universal")
@RestController
@RequestMapping("/api/v1/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService service;

    @Operation(summary = "Busca alumnos con filtros combinados (estado, idEscuela, q) y orden")
    @GetMapping
    public List<AlumnoResponse> buscar(@RequestParam(required = false) EstadoAlumno estado,
                                       @RequestParam(required = false) Long idEscuela,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "codigoEstudiante") String sortBy,
                                       @RequestParam(defaultValue = "asc") String dir) {
        return service.buscar(estado, idEscuela, q, sortBy, dir);
    }

    @Operation(summary = "Consulta un alumno por id")
    @GetMapping("/{id}")
    public AlumnoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Registra como alumno a una persona de tipo ALUMNO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlumnoResponse crear(@Valid @RequestBody AlumnoRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Actualiza codigo y fecha de ingreso de un alumno")
    @PutMapping("/{id}")
    public AlumnoResponse actualizar(@PathVariable Long id, @Valid @RequestBody AlumnoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Cambia la condicion del alumno (ACTIVO, SUSPENDIDO, RETIRADO, EGRESADO)")
    @PatchMapping("/{id}/estado")
    public AlumnoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoAlumnoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @Operation(summary = "Reporte agregado: alumnos por estado")
    @GetMapping("/resumen")
    public List<ConteoResponse> resumen() {
        return service.resumenPorEstado();
    }
}
