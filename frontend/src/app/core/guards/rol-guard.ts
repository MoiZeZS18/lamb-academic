import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth-service';

/**
 * Fabrica de guards por rol (S11 3.7). Va siempre DESPUES de authGuard en canActivate: sin sesion,
 * tieneRol daria false y mandaria a /sin-permiso en vez de ofrecer el login.
 */
export function rolGuard(...rolesPermitidos: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.tieneRol(...rolesPermitidos)) {
      return true;
    }
    return router.parseUrl('/sin-permiso');
  };
}
