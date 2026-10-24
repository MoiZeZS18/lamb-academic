package pe.edu.upeu.lambacademic.finanzas.cargo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Pago;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByCargoIdOrderByFechaPagoAsc(Long cargoId);

    boolean existsByCargoId(Long cargoId);
}
