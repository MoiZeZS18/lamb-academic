package pe.edu.upeu.lambacademic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken;
import pe.edu.upeu.lambacademic.exception.GlobalExceptionHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken.ADMIN;
import static pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken.CAJERO;
import static pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken.COORDINADOR;
import static pe.edu.upeu.lambacademic.compartido.seguridad.IdentidadToken.ESTUDIANTE;

/**
 * Resource Server de Keycloak (S10b 7.2). Las reglas por rol viven aqui; el filtro por dueno
 * (un ESTUDIANTE solo ve lo suyo) se resuelve en los controladores con IdentidadToken (ABAC).
 * Matriz completa: docs/SEGURIDAD_KEYCLOAK.md. El menu de la SPA refleja estas mismas reglas,
 * pero nunca las reemplaza (S11 2.7).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> IdentidadToken.roles(jwt).stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   JsonMapper jsonMapper) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/hello").permitAll()
                        .requestMatchers("/actuator/**").hasRole(ADMIN)

                        // Matricula (modulo propio)
                        .requestMatchers(HttpMethod.GET, "/api/v1/periodos/**").authenticated()
                        .requestMatchers("/api/v1/periodos/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/matriculas").hasAnyRole(ESTUDIANTE, COORDINADOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/matriculas/resumen", "/api/v1/matriculas/reporte")
                            .hasAnyRole(COORDINADOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/matriculas/**").hasAnyRole(ESTUDIANTE, COORDINADOR, ADMIN)
                        .requestMatchers("/api/v1/matriculas/**").hasAnyRole(COORDINADOR, ADMIN)

                        // Core Academico: la oferta se consulta con sesion; se programa desde coordinacion
                        .requestMatchers(HttpMethod.GET, "/api/v1/secciones/resumen", "/api/v1/docentes/*/carga")
                            .hasAnyRole(COORDINADOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/secciones/**", "/api/v1/cursos/**",
                                "/api/v1/docentes/**", "/api/v1/planes-estudio/**").authenticated()
                        .requestMatchers("/api/v1/secciones/**").hasAnyRole(COORDINADOR, ADMIN)
                        .requestMatchers("/api/v1/cursos/**", "/api/v1/planes-estudio/**").hasRole(ADMIN)

                        // Base Universal: estructura publica para usuarios con sesion; datos personales solo personal
                        .requestMatchers(HttpMethod.GET, "/api/v1/sedes/**", "/api/v1/facultades/**",
                                "/api/v1/escuelas/**", "/api/v1/institucional/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/personas/**", "/api/v1/alumnos/**")
                            .hasAnyRole(COORDINADOR, ADMIN, CAJERO)
                        .requestMatchers("/api/v1/expedientes/**").hasAnyRole(COORDINADOR, ADMIN)
                        .requestMatchers("/api/v1/sedes/**", "/api/v1/facultades/**", "/api/v1/escuelas/**",
                                "/api/v1/personas/**", "/api/v1/alumnos/**").hasRole(ADMIN)

                        // Finanzas: el estudiante consulta lo suyo (filtro en CargoController); caja opera
                        .requestMatchers(HttpMethod.GET, "/api/v1/finanzas/conceptos/**").authenticated()
                        .requestMatchers("/api/v1/finanzas/conceptos/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/finanzas/cargos", "/api/v1/finanzas/cargos/resumen")
                            .hasAnyRole(CAJERO, COORDINADOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/finanzas/cargos/**")
                            .hasAnyRole(ESTUDIANTE, CAJERO, COORDINADOR, ADMIN)
                        .requestMatchers("/api/v1/finanzas/**").hasAnyRole(CAJERO, ADMIN)

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(noAutenticado(jsonMapper))
                        .accessDeniedHandler(sinPermiso(jsonMapper)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(noAutenticado(jsonMapper))
                        .accessDeniedHandler(sinPermiso(jsonMapper)));
        return http.build();
    }

    /**
     * 401: sin token, token vencido o con firma invalida. Conserva la cabecera WWW-Authenticate
     * estandar (RFC 6750) y agrega el cuerpo de error comun del backend.
     */
    private AuthenticationEntryPoint noAutenticado(JsonMapper jsonMapper) {
        BearerTokenAuthenticationEntryPoint estandar = new BearerTokenAuthenticationEntryPoint();
        return (request, response, ex) -> {
            estandar.commence(request, response, ex);
            escribir(jsonMapper, request, response, HttpStatus.UNAUTHORIZED,
                    "Se requiere iniciar sesion: token ausente, vencido o invalido");
        };
    }

    /** 403: el token es valido pero el rol no alcanza; reintentar no cambia nada (S11 2.6). */
    private AccessDeniedHandler sinPermiso(JsonMapper jsonMapper) {
        return (request, response, ex) -> escribir(jsonMapper, request, response, HttpStatus.FORBIDDEN,
                "Tu rol no tiene permiso para esta operacion");
    }

    private static void escribir(JsonMapper jsonMapper, HttpServletRequest request, HttpServletResponse response,
                                 HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), GlobalExceptionHandler.cuerpo(status, mensaje, request, null));
    }
}
