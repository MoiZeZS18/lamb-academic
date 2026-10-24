package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;

/** Responde 409 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class CupoInsuficienteException extends ReglaNegocioException {

    public CupoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
