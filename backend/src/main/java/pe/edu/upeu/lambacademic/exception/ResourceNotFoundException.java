package pe.edu.upeu.lambacademic.exception;

/** Recurso inexistente: el manejador global responde 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }

    public ResourceNotFoundException(String recurso, Object id) {
        super(recurso + " no encontrado: " + id);
    }
}
