import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth-service';
import { ApiService } from '../services/api-service';

/**
 * Adjunta el token y resuelve el 401 con UN solo refresco (S11 3.8):
 *  - 401: el token vencio o no sirve -> refrescar y reintentar una vez; si no se puede, login.
 *  - 403: el token es valido pero el rol no alcanza -> se propaga tal cual, sin reintentar.
 * El reintento ocurre una sola vez: si vuelve a dar 401, el error real se propaga (sin bucle).
 * Diferencia con la guia: el token solo viaja al backend de LAMB (apiBaseUrl), a ningun otro host.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const api = inject(ApiService);

  if (!api.esDelBackend(req.url)) {
    return next(req);
  }

  const conToken = (token: string | null) =>
    token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(conToken(auth.accessToken())).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return from(auth.refrescar()).pipe(
          switchMap((renovado) => {
            if (!renovado) {
              void auth.iniciarLogin();
              return throwError(() => error);
            }
            return next(conToken(auth.accessToken()));
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
