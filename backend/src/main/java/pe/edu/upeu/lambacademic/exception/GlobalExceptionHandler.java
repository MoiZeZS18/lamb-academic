package pe.edu.upeu.lambacademic.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de errores de todos los modulos (contrato de S1 3.8):
 * 400 datos invalidos, 404 recurso inexistente, 409 conflicto de negocio, 500 no controlado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        log.warn("Regla de negocio: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest req) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(g -> errores.putIfAbsent(g.getObjectName(), g.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Error de validacion en los datos enviados", req, errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
                                                                HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Error de validacion en los parametros", req, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        log.warn("Peticion mal formada: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Peticion mal formada o con tipos invalidos", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, null);
    }

    /** Restricciones de Oracle (UNIQUE, FK, CHECK): ultima linea de defensa (S2 2.3). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex,
                                                          HttpServletRequest req) {
        log.warn("Violacion de integridad en Oracle: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "La operacion viola una restriccion de integridad de la base de datos", req, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(OptimisticLockingFailureException ex,
                                                            HttpServletRequest req) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "El registro fue modificado por otra operacion; reintente", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        log.debug("Recurso estatico no encontrado: {}", ex.getResourcePath());
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado: " + req.getRequestURI(), req, null);
    }

    /** Error no controlado: se registra completo en el log, sin exponer detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor. Reporte el traceId al equipo de soporte", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req,
                                                Map<String, String> errores) {
        return ResponseEntity.status(status).body(cuerpo(status, message, req, errores));
    }

    /** Mismo formato de error para los 401/403 que responde Spring Security (SecurityConfig). */
    public static ErrorResponse cuerpo(HttpStatus status, String message, HttpServletRequest req,
                                       Map<String, String> errores) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), MDC.get("traceId"), errores);
    }
}
