package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

/** Responde 404 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class PeriodoNoEncontradoException extends ResourceNotFoundException {

    public PeriodoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
