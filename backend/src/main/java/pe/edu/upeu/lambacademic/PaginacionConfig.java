package pe.edu.upeu.lambacademic;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Serializa Page como {"content": [...], "page": {size, number, totalElements, totalPages}}.
 * Spring Data advierte que serializar PageImpl tal cual no garantiza un JSON estable; con VIA_DTO
 * el contrato de paginacion queda fijo para el frontend.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class PaginacionConfig {
}
