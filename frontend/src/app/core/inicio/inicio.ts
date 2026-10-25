import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCalendarDays, LucideClipboardPen, LucideLayoutDashboard, LucideLock } from '@lucide/angular';
import { AuthService } from '../auth/auth-service';
import { ROLES_MATRICULA } from '../auth/roles';

/** Inicio publico (S11 2.5): sin sesion muestra el acceso; con sesion, los accesos de su rol. */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, LucideCalendarDays, LucideClipboardPen, LucideLayoutDashboard, LucideLock],
  template: `
    <section class="pagina inicio">
      <header class="encabezado-pagina">
        <div>
          <p class="migas">Sistema académico universitario</p>
          <h1>Matrícula LAMB Academic</h1>
          <p class="descripcion">
            Consulta el periodo académico vigente, elige tus cursos y secciones, revisa tu horario semanal y
            el cargo financiero de tu matrícula.
          </p>
        </div>
      </header>

      @if (!auth.autenticado()) {
        <div class="tarjeta acceso">
          <div class="cuerpo">
            <svg lucideLock class="acceso-icono" aria-hidden="true"></svg>
            <div>
              <h2>Ingreso con tu cuenta institucional</h2>
              <p class="texto-secundario">
                El inicio de sesión se realiza en el servidor de identidad de LAMB Academic. Esta aplicación nunca
                recibe tu contraseña.
              </p>
            </div>
            <button type="button" class="boton boton-primario" (click)="auth.iniciarLogin('/matricula')">
              Iniciar sesión
            </button>
          </div>
        </div>
      } @else {
        <div class="rejilla rejilla-3">
          <a class="tarjeta enlace" routerLink="/matricula">
            <div class="cuerpo">
              <svg lucideLayoutDashboard aria-hidden="true"></svg>
              <h2>Portal de matrícula</h2>
              <p class="texto-secundario">Estado del periodo y de tu matrícula actual.</p>
            </div>
          </a>
          @if (auth.tieneRol(...rolesMatricula)) {
            <a class="tarjeta enlace" routerLink="/matricula/inscripcion">
              <div class="cuerpo">
                <svg lucideClipboardPen aria-hidden="true"></svg>
                <h2>Inscripción</h2>
                <p class="texto-secundario">Selección de cursos y secciones del periodo activo.</p>
              </div>
            </a>
            <a class="tarjeta enlace" routerLink="/matricula/horario">
              <div class="cuerpo">
                <svg lucideCalendarDays aria-hidden="true"></svg>
                <h2>Horario</h2>
                <p class="texto-secundario">Horario semanal de los cursos matriculados.</p>
              </div>
            </a>
          }
        </div>
      }
    </section>
  `,
  styles: `
    .acceso .cuerpo {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 1rem 1.25rem;
    }
    .acceso .cuerpo > div {
      flex: 1 1 280px;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .acceso-icono {
      width: 28px;
      height: 28px;
      color: var(--lamb-marino-600);
    }
    .enlace {
      text-decoration: none;
      color: inherit;
    }
    .enlace:hover {
      border-color: var(--lamb-marino-600);
    }
    .enlace .cuerpo {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }
    .enlace svg {
      width: 22px;
      height: 22px;
      color: var(--lamb-turquesa-600);
    }
  `,
})
export class Inicio {
  protected readonly auth = inject(AuthService);
  protected readonly rolesMatricula = ROLES_MATRICULA;
}
