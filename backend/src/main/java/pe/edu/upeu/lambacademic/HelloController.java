package pe.edu.upeu.lambacademic;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Verificacion", description = "Endpoint de verificacion del backend (S1)")
@RestController
public class HelloController {

    @Operation(summary = "Verifica que el backend responde")
    @GetMapping("/api/v1/hello")
    public String holaMundo() {
        return "Hola LAMB Academic";
    }
}