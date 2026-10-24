package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;

/** Responde 409 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class PeriodoNoActivoException extends ReglaNegocioException {

    public PeriodoNoActivoException(String mensaje) {
        super(mensaje);
    }
}
