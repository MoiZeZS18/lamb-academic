package pe.edu.upeu.lambacademic.coreacademico.plan.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.PlanEstudio;

import java.util.List;
import java.util.Optional;

public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {

    /** Filtros combinados opcionales (escuela, estado, anio, texto, rango de creditos). */
    @Query("""
            SELECT p FROM PlanEstudio p
            WHERE (:escuelaId IS NULL OR p.escuelaId = :escuelaId)
              AND (:estado IS NULL OR p.estado = :estado)
              AND (:anio IS NULL OR p.anio = :anio)
              AND (:texto IS NULL OR LOWER(p.codigo) LIKE :texto OR LOWER(p.nombre) LIKE :texto)
              AND (:creditosMin IS NULL OR p.totalCreditos >= :creditosMin)
              AND (:creditosMax IS NULL OR p.totalCreditos <= :creditosMax)
            """)
    List<PlanEstudio> buscar(@Param("escuelaId") Long escuelaId,
                             @Param("estado") PlanEstudio.Estado estado,
                             @Param("anio") Integer anio,
                             @Param("texto") String texto,
                             @Param("creditosMin") Integer creditosMin,
                             @Param("creditosMax") Integer creditosMax,
                             Sort sort);

    @EntityGraph(attributePaths = {"detalles", "detalles.curso", "detalles.prerrequisito"})
    Optional<PlanEstudio> findDetalladoById(Long id);

    boolean existsByCodigo(String codigo);

    /** Reporte agregado de mallas por escuela y estado. */
    @Query("""
            SELECT p.escuelaId, p.estado, COUNT(p), COALESCE(SUM(p.totalCreditos), 0L)
            FROM PlanEstudio p
            GROUP BY p.escuelaId, p.estado
            ORDER BY p.escuelaId, p.estado
            """)
    List<Object[]> resumenPorEscuela();

    /** Distribucion de cursos y creditos por ciclo de una malla. */
    @Query("""
            SELECT d.ciclo, COUNT(d), COALESCE(SUM(d.creditos), 0L)
            FROM DetallePlanEstudio d
            WHERE d.planEstudio.id = :idPlan
            GROUP BY d.ciclo
            ORDER BY d.ciclo
            """)
    List<Object[]> distribucionPorCiclo(@Param("idPlan") Long idPlan);
}
