package pe.edu.upeu.lambacademic.matricula.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.lambacademic.matricula.matricula.entity.DetalleMatricula;

public interface DetalleMatriculaRepository extends JpaRepository<DetalleMatricula, Long> {

    boolean existsBySeccionId(Long seccionId);
}