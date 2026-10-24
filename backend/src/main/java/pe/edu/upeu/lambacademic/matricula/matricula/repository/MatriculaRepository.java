package pe.edu.upeu.lambacademic.matricula.matricula.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaReporteResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResumenResponse;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    @EntityGraph(attributePaths = "detalles")
    @Query("""
            SELECT m
            FROM Matricula m
            WHERE (:estudianteId IS NULL OR m.estudianteId = :estudianteId)
              AND (:periodoId IS NULL OR m.periodoId = :periodoId)
              AND (:estado IS NULL OR m.estado = :estado)
            """)
    Page<Matricula> buscar(
            @Param("estudianteId") Long estudianteId,
            @Param("periodoId") Long periodoId,
            @Param("estado") String estado,
            Pageable pageable
    );

    @Query("""
            SELECT new pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaResumenResponse(
                m.id,
                m.estudianteId,
                m.periodoId,
                m.fecha,
                m.estado,
                COUNT(d.id)
            )
            FROM Matricula m
            LEFT JOIN m.detalles d
            GROUP BY
                m.id,
                m.estudianteId,
                m.periodoId,
                m.fecha,
                m.estado
            ORDER BY m.fecha DESC
            """)
    List<MatriculaResumenResponse> buscarResumen();

    @Query("""
        SELECT new pe.edu.upeu.lambacademic.matricula.matricula.dto.MatriculaReporteResponse(
            COUNT(DISTINCT m.id),
            COALESCE(SUM(
                CASE
                    WHEN d.id IS NOT NULL THEN 1
                    ELSE 0
                END
            ), 0)
        )
        FROM Matricula m
            LEFT JOIN m.detalles d
            WHERE (:estudianteId IS NULL OR m.estudianteId = :estudianteId)
            AND (:periodoId IS NULL OR m.periodoId = :periodoId)
            AND (:estado IS NULL OR m.estado = :estado)
            """)
        MatriculaReporteResponse generarReporte(
            @Param("estudianteId") Long estudianteId,
            @Param("periodoId") Long periodoId,
            @Param("estado") String estado
    );

    /* ---------------------------- integracion ---------------------------- */

    @EntityGraph(attributePaths = "detalles")
    Optional<Matricula> findDetalladaById(Long id);

    /** Hay una matricula activa (no anulada) del estudiante en el periodo. */
    boolean existsByClaveActiva(String claveActiva);

    boolean existsByPeriodoId(Long periodoId);

    /**
     * Cursos (de la lista dada) que el estudiante ya llevo en periodos que terminaron antes
     * de la fecha indicada, en matriculas no anuladas. Se usa para validar prerrequisitos.
     */
    @Query("""
            SELECT DISTINCT d.cursoId
            FROM Matricula m JOIN m.detalles d, Periodo p
            WHERE p.id = m.periodoId
              AND m.estudianteId = :estudianteId
              AND m.estado <> 'ANULADA'
              AND d.cursoId IN :cursoIds
              AND p.fechaFin < :antesDe
            """)
    List<Long> cursosLlevadosAntesDe(@Param("estudianteId") Long estudianteId,
                                     @Param("cursoIds") Collection<Long> cursoIds,
                                     @Param("antesDe") LocalDate antesDe);
}
