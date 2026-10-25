package pe.edu.upeu.lambacademic.coreacademico.seccion.port;

/**
 * [U2] Puerto para saber si una seccion ya fue usada en alguna matricula antes de eliminarla.
 * Los detalles de matricula viven en el modulo Matricula (que depende de Core Academico): igual
 * que PeriodoAcademicoPort, Core declara el contrato y Matricula lo implementa, sin ciclo.
 * La tabla DETALLE_MATRICULA no tiene FK hacia SECCIONES (datos previos a la integracion), por
 * eso esta verificacion no puede delegarse a Oracle.
 */
public interface SeccionEnUsoPort {

    boolean tieneMatriculas(Long seccionId);
}
