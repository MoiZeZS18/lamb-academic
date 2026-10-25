package pe.edu.upeu.lambacademic.exception;

/**
 * El usuario esta autenticado pero no puede acceder a ESTE recurso (p. ej. la matricula de otro
 * estudiante): el manejador global responde 403, no 404 (S10b 7.3: existe, pero no es tuyo).
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
