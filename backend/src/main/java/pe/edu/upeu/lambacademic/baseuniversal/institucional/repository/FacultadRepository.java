package pe.edu.upeu.lambacademic.baseuniversal.institucional.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Facultad;

import java.util.List;

public interface FacultadRepository extends JpaRepository<Facultad, Long> {

    @EntityGraph(attributePaths = "sede")
    List<Facultad> findBySedeIdOrderByNombre(Long idSede);

    @EntityGraph(attributePaths = "sede")
    List<Facultad> findAllByOrderByNombre();

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsBySedeIdAndActivoTrue(Long idSede);
}
