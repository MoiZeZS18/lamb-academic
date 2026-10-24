package pe.edu.upeu.lambacademic.coreacademico.curso.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    @Query("""
            SELECT c FROM Curso c
            WHERE (:activo IS NULL OR c.activo = :activo)
              AND (:creditos IS NULL OR c.creditos = :creditos)
              AND (:texto IS NULL OR LOWER(c.codigo) LIKE :texto OR LOWER(c.nombre) LIKE :texto)
            """)
    List<Curso> buscar(@Param("activo") Boolean activo,
                       @Param("creditos") Integer creditos,
                       @Param("texto") String texto,
                       Sort sort);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
