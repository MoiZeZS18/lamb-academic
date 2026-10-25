import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideShieldAlert } from '@lucide/angular';

/** Destino de rolGuard (S11 3.7): hay sesion, pero el rol no alcanza para esa pantalla. */
@Component({
  selector: 'app-sin-permiso',
  imports: [RouterLink, LucideShieldAlert],
  template: `
    <section class="pagina">
      <div class="tarjeta-mensaje" role="alert">
        <svg lucideShieldAlert class="icono" aria-hidden="true"></svg>
        <h1>Acceso no permitido</h1>
        <p class="texto-secundario">
          Tu rol no tiene permiso para abrir esa pantalla. Si necesitas acceso, solicítalo a la coordinación
          académica.
        </p>
        <a class="boton" routerLink="/matricula">Volver al portal</a>
      </div>
    </section>
  `,
  styles: `
    .icono {
      width: 28px;
      height: 28px;
      color: var(--lamb-alerta-700);
    }
    .boton {
      align-self: flex-start;
    }
  `,
})
export class SinPermiso {}
