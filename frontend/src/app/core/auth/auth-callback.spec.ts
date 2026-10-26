import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { AuthCallback } from './auth-callback';
import { AuthService } from './auth-service';

describe('AuthCallback (retorno de Keycloak, S11 3.4)', () => {
  const auth = {
    completarLogin: vi.fn(),
    destinoPendiente: vi.fn().mockReturnValue('/matricula/inscripcion'),
  };

  async function crear(parametros: Record<string, string>) {
    await TestBed.configureTestingModule({
      imports: [AuthCallback],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(parametros) } } },
      ],
    }).compileComponents();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(AuthCallback);
    await fixture.whenStable();
    return { fixture, navegar };
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('intercambia el code con el state recibido y vuelve a la ruta que el usuario pidio', async () => {
    auth.completarLogin.mockResolvedValue(undefined);
    const { navegar } = await crear({ code: 'codigo-1', state: 'estado-1' });
    await Promise.resolve();
    expect(auth.completarLogin).toHaveBeenCalledWith('codigo-1', 'estado-1');
    expect(navegar).toHaveBeenCalledWith('/matricula/inscripcion');
  });

  it('muestra el error que devuelve Keycloak y no intenta el intercambio', async () => {
    const { fixture } = await crear({ error: 'access_denied', error_description: 'El usuario canceló el acceso.' });
    expect(auth.completarLogin).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role=alert]')!.textContent).toContain(
      'El usuario canceló el acceso.',
    );
  });

  it('sin code ni state no hay sesion que completar', async () => {
    const { fixture } = await crear({});
    expect(auth.completarLogin).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Keycloak no devolvió un código de autorización.');
  });

  it('si el state no coincide (posible CSRF) muestra el motivo y ofrece volver al inicio', async () => {
    auth.completarLogin.mockRejectedValue(new Error('La respuesta de Keycloak no corresponde a esta solicitud.'));
    const { fixture, navegar } = await crear({ code: 'codigo-1', state: 'otro' });
    await fixture.whenStable();
    fixture.detectChanges();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('La respuesta de Keycloak no corresponde a esta solicitud.');
    expect(html.querySelector('a')!.getAttribute('href')).toBe('/');
    expect(navegar).not.toHaveBeenCalled();
  });
});
