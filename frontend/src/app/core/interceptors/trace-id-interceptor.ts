import { HttpInterceptorFn } from '@angular/common/http';

/** X-Trace-ID por peticion (S1/S7): el backend lo registra en cada linea de log y lo devuelve. */
export const traceIdInterceptor: HttpInterceptorFn = (req, next) => {
  const traceId = crypto.randomUUID();

  const request = req.clone({
    setHeaders: {
      'X-Trace-ID': traceId,
    },
  });

  return next(request);
};
