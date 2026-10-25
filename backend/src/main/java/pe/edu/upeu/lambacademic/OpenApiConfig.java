package pe.edu.upeu.lambacademic;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Candado "Authorize" de Swagger UI: pega ahi un access_token de Keycloak (S10b 7.2). */
    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    OpenAPI lambAcademicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LAMB Academic API")
                        .version("v1")
                        .description("Contrato REST del sistema académico LAMB. Tokens emitidos por Keycloak "
                                + "(realm lamb-academic)."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
