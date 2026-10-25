import { Component, input } from '@angular/core';

/**
 * Estado vacio comun: explica por que no hay datos y, opcionalmente, ofrece la accion siguiente
 * (contenido proyectado). Evita repetir el mismo bloque en cada pantalla.
 */
@Component({
  selector: 'app-estado-vacio',
  template: `
    <div class="estado-vacio" role="status">
      <strong>{{ titulo() }}</strong>
      @if (descripcion()) {
        <p>{{ descripcion() }}</p>
      }
      <div class="estado-vacio-acciones">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    .estado-vacio {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.375rem;
    }
    .estado-vacio p {
      max-width: 56ch;
    }
    .estado-vacio-acciones:empty {
      display: none;
    }
    .estado-vacio-acciones {
      margin-top: 0.625rem;
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      justify-content: center;
    }
  `,
})
export class EstadoVacio {
  readonly titulo = input.required<string>();
  readonly descripcion = input<string>('');
}
