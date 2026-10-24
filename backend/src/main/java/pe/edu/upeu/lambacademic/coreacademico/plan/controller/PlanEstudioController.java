package pe.edu.upeu.lambacademic.coreacademico.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.coreacademico.curso.dto.CursoResumen;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanDistribucionResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioRequest;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanResumenResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.PlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.service.PlanEstudioService;

import java.util.List;

@Tag(name = "Malla curricular", description = "Planes de estudio y prerrequisitos (Core Academico - Curriculo)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlanEstudioController {

    private final PlanEstudioService service;

    @Operation(summary = "Crea una malla curricular (cabecera + detalles) de forma atomica")
    @PostMapping("/planes-estudio")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanEstudioResponse crear(@Valid @RequestBody PlanEstudioRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Busca mallas con filtros combinados (idEscuela, estado, anio, q, creditos) y orden")
    @GetMapping("/planes-estudio")
    public List<PlanEstudioResponse> buscar(@RequestParam(required = false) Long idEscuela,
                                            @RequestParam(required = false) PlanEstudio.Estado estado,
                                            @RequestParam(required = false) Integer anio,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) Integer creditosMin,
                                            @RequestParam(required = false) Integer creditosMax,
                                            @RequestParam(defaultValue = "anio") String sortBy,
                                            @RequestParam(defaultValue = "desc") String dir) {
        return service.buscar(idEscuela, estado, anio, q, creditosMin, creditosMax, sortBy, dir);
    }

    @Operation(summary = "Consulta una malla con sus detalles")
    @GetMapping("/planes-estudio/{id}")
    public PlanEstudioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Reporte: distribucion de cursos y creditos por ciclo")
    @GetMapping("/planes-estudio/{id}/distribucion")
    public List<PlanDistribucionResponse> distribucion(@PathVariable Long id) {
        return service.distribucion(id);
    }

    @Operation(summary = "Genera una nueva version de la malla (la anterior pasa a OBSOLETO)")
    @PostMapping("/planes-estudio/{id}/versionar")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanEstudioResponse versionar(@PathVariable Long id) {
        return service.versionar(id);
    }

    @Operation(summary = "Reporte agregado de mallas por escuela y estado")
    @GetMapping("/planes-estudio/resumen")
    public List<PlanResumenResponse> resumen() {
        return service.resumen();
    }

    @Operation(summary = "Prerrequisitos de un curso segun los planes vigentes (opcionalmente de una escuela)")
    @GetMapping("/cursos/{id}/prerrequisitos")
    public List<CursoResumen> prerrequisitos(@PathVariable Long id,
                                             @RequestParam(required = false) Long idEscuela) {
        return service.prerrequisitosDeCurso(id, idEscuela);
    }
}
