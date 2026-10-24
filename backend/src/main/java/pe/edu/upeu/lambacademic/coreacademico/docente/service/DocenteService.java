package pe.edu.upeu.lambacademic.coreacademico.docente.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lambacademic.baseuniversal.persona.dto.PersonaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.coreacademico.docente.dto.CargaDocenteResponse;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionResponse;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;

import java.time.Duration;
import java.util.List;

/**
 * Plana docente. No existe una entidad Docente en ningun aporte del equipo: el docente es
 * una Persona de tipo DOCENTE (Base Universal) y su carga se deriva de las secciones.
 */
@Service
@RequiredArgsConstructor
public class DocenteService {

    private final PersonaService personaService;
    private final SeccionService seccionService;

    @Transactional(readOnly = true)
    public List<PersonaResponse> listar(Boolean activo, String texto) {
        return personaService.listarDocentes(activo, texto);
    }

    @Transactional(readOnly = true)
    public CargaDocenteResponse carga(Long docenteId, Long periodoId) {
        PersonaResponse docente = personaService.obtenerDocente(docenteId);
        List<SeccionResponse> secciones = seccionService.buscar(periodoId, null, docenteId, null, false, "grupo", "asc")
                .stream().filter(s -> s.estado() != EstadoSeccion.CANCELADA).toList();
        double horas = secciones.stream()
                .flatMap(s -> s.horarios().stream())
                .mapToLong(h -> Duration.between(h.horaInicio(), h.horaFin()).toMinutes())
                .sum() / 60.0;
        return new CargaDocenteResponse(docente.id(), docente.nombreCompleto(), periodoId, secciones.size(),
                horas, secciones);
    }
}
