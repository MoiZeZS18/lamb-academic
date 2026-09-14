package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeccionDuplicadaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarSeccionDuplicada(
            SeccionDuplicadaException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(CupoInsuficienteException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarCupoInsuficiente(
            CupoInsuficienteException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(CruceHorarioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarCruceHorario(
            CruceHorarioException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(PeriodoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarPeriodoNoEncontrado(
            PeriodoNoEncontradoException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    public record ErrorResponse(String mensaje) {
    }
    @ExceptionHandler(EstudianteNoEncontradoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarEstudianteNoEncontrado(
            EstudianteNoEncontradoException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(EstudianteNoHabilitadoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse manejarEstudianteNoHabilitado(
            EstudianteNoHabilitadoException exception) {

        return new ErrorResponse(exception.getMessage());
    }
}