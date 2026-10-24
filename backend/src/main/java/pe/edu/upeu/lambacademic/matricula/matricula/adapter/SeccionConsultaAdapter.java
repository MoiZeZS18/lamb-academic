package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionMatriculaInfo;
import pe.edu.upeu.lambacademic.coreacademico.seccion.service.SeccionService;
import pe.edu.upeu.lambacademic.matricula.matricula.port.SeccionConsultaPort;

import java.util.List;

/**
 * Integracion real con Core Academico (antes devolvia cupos fijos 30/30 y horarios vacios).
 * Solo usa el servicio publico SeccionService, nunca el repositorio ni la entidad Seccion.
 */
@Component
@RequiredArgsConstructor
public class SeccionConsultaAdapter implements SeccionConsultaPort {

    private final SeccionService seccionService;

    @Override
    public SeccionInfo consultarSeccion(Long seccionId) {
        return seccionService.obtenerParaMatricula(seccionId)
                .map(s -> new SeccionInfo(s.seccionId(), s.limiteVacantes(), s.vacantesDisponibles(), s.cursoId(),
                        s.cursoCodigo(), s.creditos(), s.periodoId(), s.estaAbierta()))
                .orElse(null);
    }

    @Override
    public List<HorarioInfo> consultarHorarios(Long seccionId) {
        return seccionService.obtenerParaMatricula(seccionId)
                .map(SeccionMatriculaInfo::horarios)
                .orElse(List.of())
                .stream()
                .map(h -> new HorarioInfo(h.dia(), h.horaInicio(), h.horaFin()))
                .toList();
    }

    @Override
    public void reservarCupo(Long seccionId) {
        seccionService.reservarCupo(seccionId);
    }

    @Override
    public void liberarCupo(Long seccionId) {
        seccionService.liberarCupo(seccionId);
    }
}
