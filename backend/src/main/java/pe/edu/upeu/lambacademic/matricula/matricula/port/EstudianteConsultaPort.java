package pe.edu.upeu.lambacademic.matricula.matricula.port;

public interface EstudianteConsultaPort {

    EstudianteInfo consultarEstudiante(Long estudianteId);

    /**
     * escuelaId permite resolver los prerrequisitos segun el
     * plan de estudios vigente de la escuela del estudiante.
     */
    record EstudianteInfo(
            Long estudianteId,
            boolean existe,
            boolean habilitado,
            Long escuelaId
    ) {

        public static EstudianteInfo inexistente(Long estudianteId) {
            return new EstudianteInfo(estudianteId, false, false, null);
        }
    }
}
