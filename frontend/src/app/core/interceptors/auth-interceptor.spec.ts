import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../auth/auth-service';
import { authInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let token: string;
  const auth = {
    accessToken: () => token,
    refrescar: vi.fn<() => Promise<boolean>>(),
    iniciarLogin: vi.fn(),
  };

  beforeEach(() => {
    token = 'token-1';
    auth.refrescar.mockReset();
    auth.iniciarLogin.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('adjunta el Bearer a las peticiones del backend de LAMB', () => {
    http.get('http://localhost:8080/api/v1/periodos').subscribe();
    const req = backend.expectOne('http://localhost:8080/api/v1/periodos');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-1');
    req.flush([]);
  });

  it('no envia el token a otros hosts', () => {
    http.get('https://otro-servidor.example/datos').subscribe();
    const req = backend.expectOne('https://otro-servidor.example/datos');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('ante 401 refresca una vez y reintenta con el token nuevo', async () => {
    auth.refrescar.mockImplementation(async () => {
      token = 'token-2';
      return true;
    });
    let respuesta: unknown;
    http.get('http://localhost:8080/api/v1/matriculas').subscribe((r) => (respuesta = r));
    backend.expectOne('http://localhost:8080/api/v1/matriculas').flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    const reintento = backend.expectOne('http://localhost:8080/api/v1/matriculas');
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer token-2');
    reintento.flush({ content: [] });
    expect(auth.refrescar).toHaveBeenCalledTimes(1);
    expect(respuesta).toEqual({ content: [] });
  });

  it('si el reintento vuelve a dar 401, propaga el error sin un tercer intento (sin bucle)', async () => {
    auth.refrescar.mockResolvedValue(true);
    let error: HttpErrorResponse | undefined;
    http.get('http://localhost:8080/api/v1/matriculas').subscribe({ error: (e) => (error = e) });
    backend.expectOne('http://localhost:8080/api/v1/matriculas').flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    backend.expectOne('http://localhost:8080/api/v1/matriculas').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(error?.status).toBe(401);
    expect(auth.refrescar).toHaveBeenCalledTimes(1);
    backend.expectNone('http://localhost:8080/api/v1/matriculas');
  });

  it('si el refresco falla, inicia el login y propaga el 401', async () => {
    auth.refrescar.mockResolvedValue(false);
    let error: HttpErrorResponse | undefined;
    http.get('http://localhost:8080/api/v1/periodos').subscribe({ error: (e) => (error = e) });
    backend.expectOne('http://localhost:8080/api/v1/periodos').flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    await Promise.resolve();
    expect(auth.iniciarLogin).toHaveBeenCalledTimes(1);
    expect(error?.status).toBe(401);
  });

  it('403 se propaga tal cual: no refresca ni reintenta', () => {
    let error: HttpErrorResponse | undefined;
    http.post('http://localhost:8080/api/v1/periodos', {}).subscribe({ error: (e) => (error = e) });
    backend
      .expectOne('http://localhost:8080/api/v1/periodos')
      .flush({ status: 403, message: 'Tu rol no tiene permiso para esta operacion' }, { status: 403, statusText: 'Forbidden' });
    expect(error?.status).toBe(403);
    expect(auth.refrescar).not.toHaveBeenCalled();
    backend.expectNone('http://localhost:8080/api/v1/periodos');
  });
});
