package pe.edu.upeu.lambacademic.baseuniversal.institucional.controller;

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
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.ArbolResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.FacultadRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.FacultadResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.SedeRequest;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.SedeResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.service.InstitucionalService;

import java.util.List;

@Tag(name = "Institucional", description = "Arbol Sede > Facultad > Escuela (Base Universal)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InstitucionalController {

    private final InstitucionalService service;

    @Operation(summary = "Arbol completo sede > facultades > escuelas")
    @GetMapping("/institucional/arbol")
    public List<ArbolResponse> arbol() {
        return service.arbol();
    }

    /* -------------------------------- Sede ------------------------------- */

    @Operation(summary = "Lista las sedes")
    @GetMapping("/sedes")
    public List<SedeResponse> listarSedes() {
        return service.listarSedes();
    }

    @Operation(summary = "Consulta una sede por id")
    @GetMapping("/sedes/{id}")
    public SedeResponse obtenerSede(@PathVariable Long id) {
        return service.obtenerSede(id);
    }

    @Operation(summary = "Registra una sede")
    @PostMapping("/sedes")
    @ResponseStatus(HttpStatus.CREATED)
    public SedeResponse crearSede(@Valid @RequestBody SedeRequest request) {
        return service.crearSede(request);
    }

    @Operation(summary = "Actualiza una sede")
    @PutMapping("/sedes/{id}")
    public SedeResponse actualizarSede(@PathVariable Long id, @Valid @RequestBody SedeRequest request) {
        return service.actualizarSede(id, request);
    }

    @Operation(summary = "Desactiva una sede (borrado logico)")
    @DeleteMapping("/sedes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarSede(@PathVariable Long id) {
        service.desactivarSede(id);
    }

    /* ------------------------------ Facultad ----------------------------- */

    @Operation(summary = "Lista las facultades, opcionalmente de una sede")
    @GetMapping("/facultades")
    public List<FacultadResponse> listarFacultades(@RequestParam(required = false) Long idSede) {
        return service.listarFacultades(idSede);
    }

    @Operation(summary = "Consulta una facultad por id")
    @GetMapping("/facultades/{id}")
    public FacultadResponse obtenerFacultad(@PathVariable Long id) {
        return service.obtenerFacultad(id);
    }

    @Operation(summary = "Registra una facultad")
    @PostMapping("/facultades")
    @ResponseStatus(HttpStatus.CREATED)
    public FacultadResponse crearFacultad(@Valid @RequestBody FacultadRequest request) {
        return service.crearFacultad(request);
    }

    @Operation(summary = "Actualiza una facultad")
    @PutMapping("/facultades/{id}")
    public FacultadResponse actualizarFacultad(@PathVariable Long id, @Valid @RequestBody FacultadRequest request) {
        return service.actualizarFacultad(id, request);
    }

    @Operation(summary = "Desactiva una facultad (borrado logico)")
    @DeleteMapping("/facultades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarFacultad(@PathVariable Long id) {
        service.desactivarFacultad(id);
    }

    /* ------------------------------- Escuela ----------------------------- */

    @Operation(summary = "Lista las escuelas, opcionalmente de una facultad")
    @GetMapping("/escuelas")
    public List<EscuelaResponse> listarEscuelas(@RequestParam(required = false) Long idFacultad) {
        return service.listarEscuelas(idFacultad);
    }

    @Operation(summary = "Consulta una escuela por id")
    @GetMapping("/escuelas/{id}")
    public EscuelaResponse obtenerEscuela(@PathVariable Long id) {
        return service.obtenerEscuela(id);
    }

    @Operation(summary = "Registra una escuela")
    @PostMapping("/escuelas")
    @ResponseStatus(HttpStatus.CREATED)
    public EscuelaResponse crearEscuela(@Valid @RequestBody EscuelaRequest request) {
        return service.crearEscuela(request);
    }

    @Operation(summary = "Actualiza una escuela")
    @PutMapping("/escuelas/{id}")
    public EscuelaResponse actualizarEscuela(@PathVariable Long id, @Valid @RequestBody EscuelaRequest request) {
        return service.actualizarEscuela(id, request);
    }

    @Operation(summary = "Desactiva una escuela (borrado logico)")
    @DeleteMapping("/escuelas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarEscuela(@PathVariable Long id) {
        service.desactivarEscuela(id);
    }
}
