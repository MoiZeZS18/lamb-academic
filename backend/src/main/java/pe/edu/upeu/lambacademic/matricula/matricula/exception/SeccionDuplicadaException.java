package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;

/** Responde 409 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class SeccionDuplicadaException extends ReglaNegocioException {

    public SeccionDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
