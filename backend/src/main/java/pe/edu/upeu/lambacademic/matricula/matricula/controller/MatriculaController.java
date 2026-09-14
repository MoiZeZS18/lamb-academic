package pe.edu.upeu.lambacademic.matricula.matricula.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRegistroRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaReporteResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResumenResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.mapper.MatriculaMapper;
import pe.edu.upeu.lambacademic.matricula.matricula.repository.MatriculaRepository;
import pe.edu.upeu.lambacademic.matricula.matricula.service.MatriculaService;


import java.util.List;

@RestController
@RequestMapping("/api/v1/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;
    private final MatriculaRepository matriculaRepository;
    private final MatriculaMapper matriculaMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse registrar(
            @Valid @RequestBody MatriculaRegistroRequest request) {

        return matriculaService.registrar(
                request.matricula(),
                request.detalles()
        );
    }

    @GetMapping
    public Page<MatriculaResponse> buscar(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fecha") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return matriculaRepository
                .buscar(estudianteId, periodoId, estado, pageable)
                .map(matriculaMapper::toResponse);
    }

    @GetMapping("/resumen")
    public List<MatriculaResumenResponse> resumen() {
        return matriculaService.buscarResumen();
    }

    @GetMapping("/reporte")
public MatriculaReporteResponse reporte(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) String estado) {

        return matriculaService.generarReporte(
                estudianteId,
                periodoId,
                estado
        );
    }

}