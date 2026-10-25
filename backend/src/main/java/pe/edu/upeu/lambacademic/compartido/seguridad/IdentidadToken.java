package pe.edu.upeu.lambacademic.compartido.seguridad;

import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upeu.lambacademic.exception.AccesoDenegadoException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Lectura de la identidad academica desde el JWT ya validado por el Resource Server (S10b 7.3).
 *
 * El rol dice QUE puede hacer el usuario; el claim alumno_id dice SOBRE QUE datos (ABAC, RN8 de
 * BomERP aplicado a matricula). alumno_id lo publica el mapper del cliente lamb-frontend a partir
 * del atributo alumnoId del usuario en Keycloak, que solo el administrador puede editar. Nunca se
 * toma de un campo que el cliente envie en la peticion.
 */
public final class IdentidadToken {

    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String COORDINADOR = "COORDINADOR";
    public static final String ADMIN = "ADMIN";
    public static final String CAJERO = "CAJERO";
    public static final String CLAIM_ALUMNO = "alumno_id";

    /** Roles del personal: ven datos de cualquier alumno. */
    private static final List<String> PERSONAL = List.of(COORDINADOR, ADMIN, CAJERO);

    private IdentidadToken() {
    }

    public static List<String> roles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt == null ? null : jwt.getClaimAsMap("realm_access");
        Object roles = realmAccess == null ? null : realmAccess.get("roles");
        if (!(roles instanceof Collection<?> lista)) {
            return List.of();
        }
        return lista.stream().map(String::valueOf).toList();
    }

    public static boolean tieneRol(Jwt jwt, String rol) {
        return roles(jwt).contains(rol);
    }

    /**
     * Devuelve el ALUMNOS.ID al que debe limitarse la consulta, o null si el usuario es personal
     * (sin restriccion). Se usa contains y no size() == 1: Keycloak agrega sus propios roles
     * (offline_access, uma_authorization) a realm_access.roles (S10b 7.3).
     */
    public static Long estudianteRestringido(Jwt jwt) {
        List<String> roles = roles(jwt);
        if (!roles.contains(ESTUDIANTE) || roles.stream().anyMatch(PERSONAL::contains)) {
            return null;
        }
        return alumnoId(jwt);
    }

    /** ALUMNOS.ID del usuario; 403 si es un estudiante que no esta vinculado a ningun alumno. */
    public static Long alumnoId(Jwt jwt) {
        Object valor = jwt.getClaim(CLAIM_ALUMNO);
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        if (valor instanceof String texto && texto.matches("\\d{1,18}")) {
            return Long.valueOf(texto);
        }
        throw new AccesoDenegadoException("Tu cuenta no está vinculada a ningún alumno. Solicita a la administración que "
                + "registre tu código de alumno");
    }

    /** 403 si un estudiante intenta operar sobre datos de otro alumno. */
    public static void exigirPropio(Long estudianteRestringido, Long estudianteDelRecurso, String recurso) {
        if (estudianteRestringido != null && !estudianteRestringido.equals(estudianteDelRecurso)) {
            throw new AccesoDenegadoException("No tienes acceso a " + recurso + " de otro estudiante");
        }
    }
}
