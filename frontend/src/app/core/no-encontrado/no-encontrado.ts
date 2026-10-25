import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  template: `
    <section class="pagina">
      <div class="tarjeta-mensaje">
        <h1>Página no encontrada</h1>
        <p class="texto-secundario">La dirección no corresponde a ninguna pantalla de LAMB Academic.</p>
        <a class="boton" routerLink="/">Ir al inicio</a>
      </div>
    </section>
  `,
  styles: `
    .boton {
      align-self: flex-start;
    }
  `,
})
export class NoEncontrado {}
