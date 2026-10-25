import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from './auth-service';

/** Ruta de retorno de Keycloak (S11 3.4): valida state, intercambia el code y vuelve al destino. */
@Component({
  selector: 'app-auth-callback',
  imports: [RouterLink],
  template: `
    <main class="pantalla-centrada">
      @if (error()) {
        <section class="tarjeta-mensaje" role="alert">
          <h1>No se pudo completar el inicio de sesión</h1>
          <p>{{ error() }}</p>
          <a class="boton boton-primario" routerLink="/">Volver al inicio</a>
        </section>
      } @else {
        <p class="estado-carga">Completando inicio de sesión…</p>
      }
    </main>
  `,
})
export class AuthCallback {
  protected readonly error = signal<string | null>(null);

  constructor() {
    const route = inject(ActivatedRoute);
    const router = inject(Router);
    const auth = inject(AuthService);

    const codigo = route.snapshot.queryParamMap.get('code');
    const state = route.snapshot.queryParamMap.get('state');
    const errorKeycloak = route.snapshot.queryParamMap.get('error');

    if (errorKeycloak || !codigo || !state) {
      this.error.set(
        route.snapshot.queryParamMap.get('error_description') ??
          'Keycloak no devolvió un código de autorización.',
      );
      return;
    }

    auth
      .completarLogin(codigo, state)
      .then(() => router.navigateByUrl(auth.destinoPendiente()))
      .catch((e: Error) => this.error.set(e.message));
  }
}
