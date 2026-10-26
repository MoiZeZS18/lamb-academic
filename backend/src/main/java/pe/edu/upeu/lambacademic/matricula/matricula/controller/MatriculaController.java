package pe.edu.upeu.lambacademic.matricula.matricula.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken;

import pe.edu.upeu.lambacademic.matricula.matricula.dto.ContextoAcademicoResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaRegistroRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaReporteResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResumenResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.ResultadoCursoRequest;
import pe.edu.upeu.lambacademic.matricula.matricula.service.MatriculaService;


import java.util.List;

@Tag(name = "Matriculas", description = "Registro transaccional de matricula (cabecera-detalle)")
@RestController
@RequestMapping("/api/v1/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;

    /*
     * [U2, S10b 7.3] Un ESTUDIANTE solo opera sobre su propia matricula: su ALUMNOS.ID sale del
     * claim alumno_id del token ya validado, nunca de un campo que el cliente pueda cambiar.
     * COORDINADOR / ADMIN no tienen esa restriccion (estudianteRestringido devuelve null).
     */

    @Operation(summary = "Registra una matricula con sus secciones, reserva cupos y genera el cargo financiero")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse registrar(
            @Valid @RequestBody MatriculaRegistroRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        IdentidadToken.exigirPropio(IdentidadToken.estudianteRestringido(jwt),
                request.matricula().estudianteId(), "la matrícula");
        return matriculaService.registrar(
                request.matricula(),
                request.detalles()
        );
    }

    /**
     * [U2] Contexto academico (escuela, habilitacion) para la inscripcion. El ESTUDIANTE solo obtiene
     * el suyo: si no envia estudianteId se usa el del token y pedir el de otro alumno es un 403.
     * El personal debe indicar el alumno.
     */
    @Operation(summary = "Escuela y habilitacion del alumno para armar su inscripcion")
    @GetMapping("/contexto")
    public ContextoAcademicoResponse contexto(
            @RequestParam(required = false) Long estudianteId,
            @AuthenticationPrincipal Jwt jwt) throws MissingServletRequestParameterException {

        Long propio = IdentidadToken.estudianteRestringido(jwt);
        if (propio != null) {
            IdentidadToken.exigirPropio(propio, estudianteId == null ? propio : estudianteId, "el contexto académico");
            estudianteId = propio;
        } else if (estudianteId == null) {
            throw new MissingServletRequestParameterException("estudianteId", "Long");
        }
        return matriculaService.contextoAcademico(estudianteId);
    }

    @Operation(summary = "Busca matriculas con filtros combinados, paginacion y orden")
    @GetMapping
    public Page<MatriculaResponse> buscar(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fecha") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt) {

        Long propio = IdentidadToken.estudianteRestringido(jwt);
        if (propio != null) {
            // Pedir expresamente las de otro alumno es un 403; sin filtro, solo se listan las propias.
            IdentidadToken.exigirPropio(propio, estudianteId == null ? propio : estudianteId, "las matrículas");
            estudianteId = propio;
        }
        return matriculaService.buscar(estudianteId, periodoId, estado, page, size, sortBy, direction);
    }

    @Operation(summary = "Resumen: cada matricula con su cantidad de detalles")
    @GetMapping("/resumen")
    public List<MatriculaResumenResponse> resumen() {
        return matriculaService.buscarResumen();
    }

    @Operation(summary = "Reporte agregado: total de matriculas y detalles con filtros opcionales")
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

    @Operation(summary = "Consulta una matricula con sus detalles")
    @GetMapping("/{id}")
    public MatriculaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return matriculaService.obtener(id, IdentidadToken.estudianteRestringido(jwt));
    }

    @Operation(summary = "Anula una matricula: libera cupos y anula su cargo (si no tiene pagos)")
    @PatchMapping("/{id}/anular")
    public MatriculaResponse anular(@PathVariable Long id) {
        return matriculaService.anular(id);
    }

    @Operation(summary = "Registra el resultado final (APROBADO, DESAPROBADO, RETIRADO) de un curso; periodo CERRADO")
    @PatchMapping("/{id}/detalles/{detalleId}/resultado")
    public MatriculaResponse registrarResultado(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @Valid @RequestBody ResultadoCursoRequest request) {
        return matriculaService.registrarResultado(id, detalleId, request.resultado());
    }

    @Operation(summary = "Genera (idempotente) el cargo financiero de una matricula que no lo tenga")
    @PostMapping("/{id}/cargo")
    public MatriculaResponse generarCargo(
            @PathVariable Long id,
            @RequestParam(required = false) @Min(1) @Max(12) Integer numeroCuotas) {
        return matriculaService.generarCargo(id, numeroCuotas);
    }

}
