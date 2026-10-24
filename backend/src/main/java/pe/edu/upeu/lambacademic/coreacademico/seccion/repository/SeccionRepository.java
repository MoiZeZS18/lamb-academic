package pe.edu.upeu.lambacademic.coreacademico.seccion.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionOcupacionReporte;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.DiaSemana;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.HorarioAula;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.Seccion;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    @EntityGraph(attributePaths = {"curso", "horarios"})
    @Query("""
            SELECT s FROM Seccion s
            WHERE (:periodoId IS NULL OR s.periodoId = :periodoId)
              AND (:cursoId IS NULL OR s.curso.id = :cursoId)
              AND (:docenteId IS NULL OR s.docenteId = :docenteId)
              AND (:estado IS NULL OR s.estado = :estado)
              AND (:soloConVacantes = 0 OR s.cuposOcupados < s.limiteVacantes)
            """)
    List<Seccion> buscar(@Param("periodoId") Long periodoId,
                         @Param("cursoId") Long cursoId,
                         @Param("docenteId") Long docenteId,
                         @Param("estado") EstadoSeccion estado,
                         @Param("soloConVacantes") int soloConVacantes,
                         Sort sort);

    @EntityGraph(attributePaths = {"curso", "horarios"})
    Optional<Seccion> findDetalladaById(Long id);

    boolean existsByPeriodoIdAndCursoIdAndGrupo(Long periodoId, Long cursoId, String grupo);

    /**
     * Reserva atomica de un cupo: el UPDATE solo afecta la fila si la seccion esta ABIERTA y
     * aun tiene vacantes. Oracle bloquea la fila hasta el commit, por lo que dos matriculas
     * concurrentes no pueden ocupar el ultimo cupo a la vez. Devuelve 1 si reservo, 0 si no.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE SECCIONES SET CUPOS_OCUPADOS = CUPOS_OCUPADOS + 1
            WHERE ID = :id AND ESTADO = 'ABIERTA' AND CUPOS_OCUPADOS < LIMITE_VACANTES
            """, nativeQuery = true)
    int reservarCupo(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE SECCIONES SET CUPOS_OCUPADOS = CUPOS_OCUPADOS - 1 WHERE ID = :id AND CUPOS_OCUPADOS > 0",
            nativeQuery = true)
    int liberarCupo(@Param("id") Long id);

    /** Horarios de otras secciones del periodo que ocupan la misma aula en un rango que se cruza. */
    @Query("""
            SELECT h FROM HorarioAula h JOIN FETCH h.seccion s JOIN FETCH s.curso
            WHERE s.periodoId = :periodoId
              AND s.estado <> :cancelada
              AND (:excluirSeccionId IS NULL OR s.id <> :excluirSeccionId)
              AND h.diaSemana = :dia
              AND UPPER(h.aula) = UPPER(:aula)
              AND h.horaInicio < :fin AND h.horaFin > :inicio
            """)
    List<HorarioAula> cruceDeAula(@Param("periodoId") Long periodoId,
                                  @Param("excluirSeccionId") Long excluirSeccionId,
                                  @Param("dia") DiaSemana dia,
                                  @Param("aula") String aula,
                                  @Param("inicio") LocalTime inicio,
                                  @Param("fin") LocalTime fin,
                                  @Param("cancelada") EstadoSeccion cancelada);

    /** Horarios de otras secciones del mismo docente en el periodo que se cruzan. */
    @Query("""
            SELECT h FROM HorarioAula h JOIN FETCH h.seccion s JOIN FETCH s.curso
            WHERE s.periodoId = :periodoId
              AND s.estado <> :cancelada
              AND (:excluirSeccionId IS NULL OR s.id <> :excluirSeccionId)
              AND s.docenteId = :docenteId
              AND h.diaSemana = :dia
              AND h.horaInicio < :fin AND h.horaFin > :inicio
            """)
    List<HorarioAula> cruceDeDocente(@Param("periodoId") Long periodoId,
                                     @Param("excluirSeccionId") Long excluirSeccionId,
                                     @Param("docenteId") Long docenteId,
                                     @Param("dia") DiaSemana dia,
                                     @Param("inicio") LocalTime inicio,
                                     @Param("fin") LocalTime fin,
                                     @Param("cancelada") EstadoSeccion cancelada);

    /** Reporte agregado de ocupacion por curso en un periodo (S5 2.5). */
    @Query("""
            SELECT new pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionOcupacionReporte(
                c.codigo, c.nombre, COUNT(s), SUM(s.limiteVacantes), SUM(s.cuposOcupados))
            FROM Seccion s JOIN s.curso c
            WHERE s.periodoId = :periodoId AND s.estado <> :cancelada
            GROUP BY c.codigo, c.nombre
            ORDER BY c.codigo
            """)
    List<SeccionOcupacionReporte> ocupacionPorCurso(@Param("periodoId") Long periodoId,
                                                    @Param("cancelada") EstadoSeccion cancelada);
}
