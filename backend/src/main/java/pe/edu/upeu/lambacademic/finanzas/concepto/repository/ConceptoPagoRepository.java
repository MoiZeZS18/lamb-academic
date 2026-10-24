package pe.edu.upeu.lambacademic.finanzas.concepto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;

import java.util.Collection;
import java.util.List;

public interface ConceptoPagoRepository extends JpaRepository<ConceptoPago, Long> {

    List<ConceptoPago> findAllByOrderByCodigo();

    List<ConceptoPago> findByActivoTrueAndAplicaMatriculaTrueOrderByCodigo();

    List<ConceptoPago> findByCodigoIn(Collection<String> codigos);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
