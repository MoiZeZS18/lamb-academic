package pe.edu.upeu.lambacademic;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken;

import java.util.List;

/**
 * Perfil del usuario autenticado leido directamente del Jwt, sin consultar ninguna base
 * (S10 4.1, punto 3): sirve para verificar que lo que ve la SPA coincide con lo que valida el backend.
 */
@Tag(name = "Autenticacion", description = "Identidad del usuario segun el token de Keycloak")
@RestController
@RequestMapping("/api/v1/auth")
public class PerfilController {

    public record PerfilResponse(String sub, String usuario, String nombre, String email,
                                 List<String> roles, Long alumnoId) {
    }

    @Operation(summary = "Perfil del token: usuario, roles del realm y alumno vinculado")
    @GetMapping("/perfil")
    public PerfilResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        Long alumnoId = jwt.hasClaim(IdentidadToken.CLAIM_ALUMNO) ? IdentidadToken.alumnoId(jwt) : null;
        List<String> roles = IdentidadToken.roles(jwt).stream()
                .filter(r -> List.of(IdentidadToken.ESTUDIANTE, IdentidadToken.COORDINADOR, IdentidadToken.ADMIN,
                        IdentidadToken.CAJERO).contains(r))
                .toList();
        return new PerfilResponse(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"), jwt.getClaimAsString("email"), roles, alumnoId);
    }
}
