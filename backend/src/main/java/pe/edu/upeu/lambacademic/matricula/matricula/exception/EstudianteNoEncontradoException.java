package pe.edu.upeu.lambacademic.matricula.matricula.exception;

import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

/** Responde 404 a traves del manejador global (pe.edu.upeu.lambacademic.exception). */
public class EstudianteNoEncontradoException extends ResourceNotFoundException {

    public EstudianteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
