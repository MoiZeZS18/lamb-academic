package pe.edu.upeu.lambacademic.exception;

/**
 * Regla de negocio violada (conflicto con el estado actual del sistema): responde 409.
 * Extiende RuntimeException para que @Transactional haga rollback automatico (S4 2.5).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
