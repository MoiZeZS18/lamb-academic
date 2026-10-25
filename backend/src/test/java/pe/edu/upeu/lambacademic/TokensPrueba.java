package pe.edu.upeu.lambacademic;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Tokens de prueba con la misma forma que los de Keycloak: realm_access.roles (incluidos los roles
 * propios del realm, que no deben confundir al filtro por dueno) y alumno_id para ESTUDIANTE.
 */
public final class TokensPrueba {

    private TokensPrueba() {
    }

    public static RequestPostProcessor estudiante(long alumnoId) {
        return token(alumnoId, "ESTUDIANTE");
    }

    /** ESTUDIANTE al que el administrador no le asigno el atributo alumnoId en Keycloak. */
    public static RequestPostProcessor estudianteSinAlumno() {
        return token(null, "ESTUDIANTE");
    }

    public static RequestPostProcessor coordinador() {
        return token(null, "COORDINADOR");
    }

    public static RequestPostProcessor admin() {
        return token(null, "ADMIN");
    }

    public static RequestPostProcessor cajero() {
        return token(null, "CAJERO");
    }

    private static RequestPostProcessor token(Long alumnoId, String... rolesPropios) {
        List<String> roles = new ArrayList<>(Arrays.asList(rolesPropios));
        roles.addAll(List.of("offline_access", "uma_authorization", "default-roles-lamb-academic"));
        return jwt()
                .jwt(j -> {
                    j.subject("sub-" + String.join("-", rolesPropios).toLowerCase())
                            .claim("preferred_username", "usuario.prueba")
                            .claim("realm_access", Map.of("roles", roles));
                    if (alumnoId != null) {
                        j.claim("alumno_id", alumnoId);
                    }
                })
                .authorities(roles.stream().map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r)).toList());
    }
}
