package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;

/** Responde 409 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class SeccionNoDisponibleException extends ReglaNegocioException {

    public SeccionNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
