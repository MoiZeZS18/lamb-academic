package pe.edu.upeu.lambacademic.coreacademico.seccion.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Contrato publico de Core Academico para Matricula: lo necesario para validar una
 * seccion (periodo, estado, cupos, horarios) sin exponer las entidades Seccion ni Curso.
 */
public record SeccionMatriculaInfo(
        Long seccionId,
        Long cursoId,
        String cursoCodigo,
        String cursoNombre,
        Integer creditos,
        Long periodoId,
        String grupo,
        Integer limiteVacantes,
        Integer cuposOcupados,
        String estado,
        List<Horario> horarios
) {

    public boolean estaAbierta() {
        return "ABIERTA".equals(estado);
    }

    public int vacantesDisponibles() {
        return limiteVacantes - cuposOcupados;
    }

    public record Horario(String dia, LocalTime horaInicio, LocalTime horaFin) {
    }
}
