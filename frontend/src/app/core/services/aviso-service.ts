import { Injectable, signal } from '@angular/core';

export type TipoAviso = 'error' | 'info' | 'ok';

export interface Aviso {
  id: number;
  texto: string;
  tipo: TipoAviso;
}

/**
 * Avisos breves de la SPA (confirmaciones y errores que no pertenecen a un formulario).
 * Reutilizado del proyecto de Cristian (core/aviso.service.ts): mismo contrato con signals.
 */
@Injectable({ providedIn: 'root' })
export class AvisoService {
  readonly avisos = signal<Aviso[]>([]);
  private secuencia = 0;

  mostrar(texto: string, tipo: TipoAviso = 'info', milisegundos = 6000): void {
    const id = ++this.secuencia;
    this.avisos.update((lista) => [...lista, { id, texto, tipo }]);
    setTimeout(() => this.cerrar(id), milisegundos);
  }

  cerrar(id: number): void {
    this.avisos.update((lista) => lista.filter((a) => a.id !== id));
  }
}
