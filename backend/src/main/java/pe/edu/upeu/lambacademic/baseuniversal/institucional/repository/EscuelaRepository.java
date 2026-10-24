package pe.edu.upeu.lambacademic.baseuniversal.institucional.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.entity.Escuela;

import java.util.List;

public interface EscuelaRepository extends JpaRepository<Escuela, Long> {

    @EntityGraph(attributePaths = "facultad")
    List<Escuela> findByFacultadIdOrderByNombre(Long idFacultad);

    @EntityGraph(attributePaths = "facultad")
    List<Escuela> findAllByOrderByNombre();

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByFacultadIdAndActivoTrue(Long idFacultad);
}
