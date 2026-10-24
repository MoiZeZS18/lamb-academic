package pe.edu.upeu.lambacademic.finanzas.cargo.dto;

/** Contrato publico: datos que Matricula envia para generar el cargo de una matricula. */
public record CargoMatriculaCommand(
        Long matriculaId,
        Long estudianteId,
        Long periodoId,
        String periodoCodigo,
        Integer totalCreditos,
        Integer numeroCuotas
) {
}
