package pe.edu.upeu.lambacademic.matricula.periodo.repository;

import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}