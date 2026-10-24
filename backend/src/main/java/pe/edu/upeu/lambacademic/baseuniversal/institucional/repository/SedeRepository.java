package pe.edu.upeu.lambacademic.baseuniversal.institucional.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Sede;

public interface SedeRepository extends JpaRepository<Sede, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
