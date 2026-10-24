package pe.edu.upeu.lambacademic;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica automaticamente los limites del monolito modular (ADR-002, S1 3.4.4): ningun
 * modulo accede a los paquetes internos de otro y no hay dependencias ciclicas.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(LambAcademicApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void detectaLosModulosDeNegocio() {
        assertThat(modules.stream().map(ApplicationModule::getName))
                .contains("baseuniversal", "coreacademico", "matricula", "finanzas");
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
