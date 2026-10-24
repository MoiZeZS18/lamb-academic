package pe.edu.upeu.lambacademic.coreacademico.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.DetallePlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.PlanEstudio;

import java.util.List;

public interface DetallePlanEstudioRepository extends JpaRepository<DetallePlanEstudio, Long> {

    /**
     * Prerrequisitos de un curso segun los planes VIGENTES (de una escuela o de todas).
     * Consulta explicita bajo demanda: navegacion controlada (S3 2.5).
     */
    @Query("""
            SELECT DISTINCT d.prerrequisito FROM DetallePlanEstudio d
            WHERE d.curso.id = :cursoId
              AND d.prerrequisito IS NOT NULL
              AND d.planEstudio.estado = :vigente
              AND (:escuelaId IS NULL OR d.planEstudio.escuelaId = :escuelaId)
            """)
    List<Curso> prerrequisitosDeCurso(@Param("cursoId") Long cursoId,
                                      @Param("escuelaId") Long escuelaId,
                                      @Param("vigente") PlanEstudio.Estado vigente);
}
