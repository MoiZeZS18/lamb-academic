import { Component, computed, input } from '@angular/core';

/** Estados reales de los contratos del backend y su presentacion. */
const PRESENTACION: Record<string, { texto: string; clase: string }> = {
  ACTIVO: { texto: 'Activo', clase: 'estado-activo' },
  PLANIFICADO: { texto: 'Planificado', clase: 'estado-info' },
  CERRADO: { texto: 'Cerrado', clase: 'estado-neutro' },
  ABIERTA: { texto: 'Abierta', clase: 'estado-activo' },
  CERRADA: { texto: 'Cerrada', clase: 'estado-neutro' },
  CANCELADA: { texto: 'Cancelada', clase: 'estado-error' },
  INICIADA: { texto: 'Iniciada', clase: 'estado-info' },
  CONFIRMADA: { texto: 'Confirmada', clase: 'estado-activo' },
  ANULADA: { texto: 'Anulada', clase: 'estado-error' },
  EMITIDO: { texto: 'Emitido', clase: 'estado-info' },
  PAGADO_PARCIAL: { texto: 'Pago parcial', clase: 'estado-alerta' },
  PAGADO_TOTAL: { texto: 'Pagado', clase: 'estado-activo' },
  ANULADO: { texto: 'Anulado', clase: 'estado-error' },
  PENDIENTE: { texto: 'Pendiente', clase: 'estado-neutro' },
  PAGADA_PARCIAL: { texto: 'Pago parcial', clase: 'estado-alerta' },
  PAGADA: { texto: 'Pagada', clase: 'estado-activo' },
  APROBADO: { texto: 'Aprobado', clase: 'estado-activo' },
  DESAPROBADO: { texto: 'Desaprobado', clase: 'estado-error' },
  RETIRADO: { texto: 'Retirado', clase: 'estado-neutro' },
};

@Component({
  selector: 'app-etiqueta-estado',
  template: `<span class="etiqueta-estado" [class]="'etiqueta-estado ' + presentacion().clase">{{
    presentacion().texto
  }}</span>`,
})
export class EtiquetaEstado {
  readonly estado = input.required<string>();

  protected readonly presentacion = computed(
    () => PRESENTACION[this.estado()] ?? { texto: this.estado(), clase: 'estado-neutro' },
  );
}
