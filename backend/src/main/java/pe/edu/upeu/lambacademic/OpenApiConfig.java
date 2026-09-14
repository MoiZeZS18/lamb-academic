package pe.edu.upeu.lambacademic;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lambAcademicOpenApi() {
        return new OpenAPI().info(new Info()
                .title("LAMB Academic API")
                .version("v1")
                .description("Contrato REST del sistema académico LAMB"));
    }
}