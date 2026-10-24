package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;

/** Responde 409 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class PrerrequisitoNoCumplidoException extends ReglaNegocioException {

    public PrerrequisitoNoCumplidoException(String mensaje) {
        super(mensaje);
    }
}
