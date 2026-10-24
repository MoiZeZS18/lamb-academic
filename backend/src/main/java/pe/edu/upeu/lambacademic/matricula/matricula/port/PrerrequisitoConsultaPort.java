package pe.edu.upeu.lambacademic.matricula.matricula.port;

import java.util.List;

/** Prerrequisitos de un curso segun la malla curricular vigente (Core Academico). */
public interface PrerrequisitoConsultaPort {

    List<PrerrequisitoInfo> prerrequisitosDe(Long cursoId, Long escuelaId);

    record PrerrequisitoInfo(Long cursoId, String codigo) {
    }
}
