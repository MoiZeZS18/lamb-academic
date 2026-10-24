package pe.edu.upeu.lambacademic.baseuniversal.expediente.dto;

import pe.edu.upeu.lambacademic.baseuniversal.expediente.entity.EstadoExpediente;

/** Fila del reporte agregado de expedientes por estado. */
public record ExpedienteEstadoReporte(EstadoExpediente estado, Long cantidadExpedientes, Long totalDocumentos) {
}
