package pe.edu.upeu.lambacademic.finanzas.cargo.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoEstadoReporte;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResumenResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Cargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.EstadoCuota;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.ProcesoOrigen;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    /** Proyeccion de listado (sin cuotas) con filtros combinados opcionales. */
    @Query("""
            SELECT new pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResumenResponse(
                c.id, c.numeroCargo, c.estudianteId, c.procesoOrigen, c.referenciaId, c.periodoId,
                c.montoTotal, c.saldoPendiente, c.numeroCuotas, c.estado, c.fechaEmision)
            FROM Cargo c
            WHERE (:estudianteId IS NULL OR c.estudianteId = :estudianteId)
              AND (:periodoId IS NULL OR c.periodoId = :periodoId)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:procesoOrigen IS NULL OR c.procesoOrigen = :procesoOrigen)
            """)
    List<CargoResumenResponse> buscar(@Param("estudianteId") Long estudianteId,
                                      @Param("periodoId") Long periodoId,
                                      @Param("estado") EstadoCargo estado,
                                      @Param("procesoOrigen") ProcesoOrigen procesoOrigen,
                                      Sort sort);

    @EntityGraph(attributePaths = "cuotas")
    Optional<Cargo> findDetalladoById(Long id);

    @EntityGraph(attributePaths = "cuotas")
    Optional<Cargo> findByProcesoOrigenAndReferenciaId(ProcesoOrigen procesoOrigen, Long referenciaId);

    @EntityGraph(attributePaths = "cuotas")
    List<Cargo> findByEstudianteIdOrderByFechaEmisionDesc(Long estudianteId);

    /** Cargos de varias referencias (p. ej. una pagina de matriculas) en una sola consulta. */
    List<Cargo> findByProcesoOrigenAndReferenciaIdIn(ProcesoOrigen procesoOrigen, Collection<Long> referenciaIds);

    /** Saldo consolidado del estudiante (los cargos anulados no cuentan como deuda). */
    @Query("""
            SELECT COALESCE(SUM(c.montoTotal), 0BD), COALESCE(SUM(c.saldoPendiente), 0BD),
                   SUM(CASE WHEN c.estado IN (:emitido, :parcial) THEN 1 ELSE 0 END)
            FROM Cargo c
            WHERE c.estudianteId = :estudianteId AND c.estado <> :anulado
            """)
    List<Object[]> saldoDeEstudiante(@Param("estudianteId") Long estudianteId,
                                     @Param("emitido") EstadoCargo emitido,
                                     @Param("parcial") EstadoCargo parcial,
                                     @Param("anulado") EstadoCargo anulado);

    @Query("""
            SELECT COUNT(d) FROM DetalleCargo d
            WHERE d.cargo.estudianteId = :estudianteId
              AND d.estado IN (:pendiente, :parcial)
              AND d.fechaVencimiento < :hoy
            """)
    long cuotasVencidas(@Param("estudianteId") Long estudianteId,
                        @Param("pendiente") EstadoCuota pendiente,
                        @Param("parcial") EstadoCuota parcial,
                        @Param("hoy") LocalDate hoy);

    /** Reporte agregado por estado del cargo (opcionalmente de un periodo). */
    @Query("""
            SELECT new pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoEstadoReporte(
                c.estado, COUNT(c), COALESCE(SUM(c.montoTotal), 0BD), COALESCE(SUM(c.saldoPendiente), 0BD))
            FROM Cargo c
            WHERE (:periodoId IS NULL OR c.periodoId = :periodoId)
            GROUP BY c.estado
            ORDER BY c.estado
            """)
    List<CargoEstadoReporte> resumenPorEstado(@Param("periodoId") Long periodoId);
}
