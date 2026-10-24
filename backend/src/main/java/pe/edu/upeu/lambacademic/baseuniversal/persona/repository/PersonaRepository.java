package pe.edu.upeu.lambacademic.baseuniversal.persona.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.baseuniversal.persona.entity.Persona;

import java.util.List;
import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /** Filtros combinados opcionales en una sola consulta (S5 2.2) + ordenamiento dinamico. */
    @EntityGraph(attributePaths = "escuela")
    @Query("""
            SELECT p FROM Persona p
            WHERE (:tipoPersona IS NULL OR p.tipoPersona = :tipoPersona)
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:idEscuela IS NULL OR p.escuela.id = :idEscuela)
              AND (:texto IS NULL
                   OR LOWER(p.numeroDocumento) LIKE :texto
                   OR LOWER(p.nombres) LIKE :texto
                   OR LOWER(p.apellidoPaterno) LIKE :texto
                   OR LOWER(p.apellidoMaterno) LIKE :texto
                   OR LOWER(p.email) LIKE :texto)
            """)
    List<Persona> buscar(@Param("tipoPersona") Persona.TipoPersona tipoPersona,
                         @Param("activo") Boolean activo,
                         @Param("idEscuela") Long idEscuela,
                         @Param("texto") String texto,
                         Sort sort);

    @EntityGraph(attributePaths = "escuela")
    Optional<Persona> findWithEscuelaById(Long id);

    @EntityGraph(attributePaths = "escuela")
    Optional<Persona> findByTipoDocumentoAndNumeroDocumento(Persona.TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(Persona.TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(Persona.TipoDocumento tipoDocumento,
                                                            String numeroDocumento, Long id);

    /** Agregacion: personas activas por tipo. */
    @Query("SELECT p.tipoPersona, COUNT(p) FROM Persona p WHERE p.activo = TRUE GROUP BY p.tipoPersona ORDER BY 2 DESC")
    List<Object[]> contarPorTipoPersona();

    /** Agregacion: personas activas por escuela. */
    @Query("""
            SELECT COALESCE(e.nombre, 'Sin escuela'), COUNT(p)
            FROM Persona p LEFT JOIN p.escuela e
            WHERE p.activo = TRUE
            GROUP BY e.nombre
            ORDER BY 2 DESC
            """)
    List<Object[]> contarPorEscuela();
}
