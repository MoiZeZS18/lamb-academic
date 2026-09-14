package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PeriodoNoEncontradoException extends RuntimeException {

    public PeriodoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}