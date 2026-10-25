import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo de error comun del backend (ErrorResponse / GlobalExceptionHandler). */
export interface ErrorApi {
  timestamp?: string;
  status: number;
  error?: string;
  message?: string;
  path?: string;
  traceId?: string;
  errores?: Record<string, string>;
}

export function cuerpoError(err: HttpErrorResponse): ErrorApi | null {
  const cuerpo = err.error;
  return cuerpo && typeof cuerpo === 'object' && 'status' in cuerpo ? (cuerpo as ErrorApi) : null;
}

/**
 * Traduce un error HTTP a un mensaje especifico para la pantalla (S9 3.9: nunca un mensaje
 * generico si el backend ya explico la causa). `generico` se usa solo cuando no hay explicacion.
 */
export function mensajeHttp(err: HttpErrorResponse, generico: string): string {
  const cuerpo = cuerpoError(err);
  switch (err.status) {
    case 0:
      return 'No se pudo conectar con el servidor de LAMB Academic. Verifica que esté en ejecución.';
    case 401:
      return 'Tu sesión expiró. Vuelve a iniciar sesión.';
    case 403:
      // El backend ya explica el motivo (rol insuficiente o recurso de otro estudiante).
      return cuerpo?.message ?? 'Tu rol no tiene permiso para esta operación.';
    case 400:
      if (cuerpo?.errores && Object.keys(cuerpo.errores).length) {
        return 'Datos no válidos: ' + Object.entries(cuerpo.errores).map(([c, m]) => `${c}: ${m}`).join('; ');
      }
      return cuerpo?.message ?? 'Los datos enviados no son válidos.';
    case 404:
    case 409:
      return cuerpo?.message ?? generico;
    default:
      return cuerpo?.traceId ? `${generico} (código de rastreo ${cuerpo.traceId})` : generico;
  }
}
