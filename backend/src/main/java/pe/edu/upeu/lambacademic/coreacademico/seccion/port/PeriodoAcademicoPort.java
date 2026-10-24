package pe.edu.upeu.lambacademic.coreacademico.seccion.port;

import java.util.Optional;

/**
 * Puerto que Core Academico necesita para validar el periodo de una seccion.
 * El Periodo vive en el modulo Matricula, que a su vez depende de Core Academico; para
 * no crear un ciclo, Core declara este contrato y Matricula lo implementa (inversion de
 * dependencias, verificada por ModularityTests).
 */
public interface PeriodoAcademicoPort {

    Optional<PeriodoInfo> buscarPeriodo(Long periodoId);

    record PeriodoInfo(Long id, String codigo, String nombre, String estado) {

        public boolean estaCerrado() {
            return "CERRADO".equalsIgnoreCase(estado);
        }
    }
}
