package pe.edu.upeu.lambacademic.baseuniversal.alumno.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.Alumno;
import pe.edu.upeu.lambacademic.baseuniversal.alumno.entity.EstadoAlumno;

import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    @EntityGraph(attributePaths = {"persona", "persona.escuela"})
    @Query("""
            SELECT a FROM Alumno a JOIN a.persona p
            WHERE (:estado IS NULL OR a.estado = :estado)
              AND (:idEscuela IS NULL OR p.escuela.id = :idEscuela)
              AND (:texto IS NULL
                   OR LOWER(a.codigoEstudiante) LIKE :texto
                   OR LOWER(p.numeroDocumento) LIKE :texto
                   OR LOWER(p.nombres) LIKE :texto
                   OR LOWER(p.apellidoPaterno) LIKE :texto
                   OR LOWER(p.apellidoMaterno) LIKE :texto)
            """)
    List<Alumno> buscar(@Param("estado") EstadoAlumno estado,
                        @Param("idEscuela") Long idEscuela,
                        @Param("texto") String texto,
                        Sort sort);

    @EntityGraph(attributePaths = {"persona", "persona.escuela"})
    Optional<Alumno> findWithPersonaById(Long id);

    boolean existsByPersonaId(Long personaId);

    boolean existsByCodigoEstudiante(String codigoEstudiante);

    boolean existsByCodigoEstudianteAndIdNot(String codigoEstudiante, Long id);

    @Query("SELECT a.estado, COUNT(a) FROM Alumno a GROUP BY a.estado ORDER BY 2 DESC")
    List<Object[]> contarPorEstado();
}
