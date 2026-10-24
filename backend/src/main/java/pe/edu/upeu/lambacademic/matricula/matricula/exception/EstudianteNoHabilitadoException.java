package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;

/** Responde 409 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class EstudianteNoHabilitadoException extends ReglaNegocioException {

    public EstudianteNoHabilitadoException(String mensaje) {
        super(mensaje);
    }
}
