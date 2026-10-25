import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth-service';
import { authGuard } from './auth-guard';
import { rolGuard } from './rol-guard';

describe('guards', () => {
  const auth = { autenticado: vi.fn(), tieneRol: vi.fn(), iniciarLogin: vi.fn().mockResolvedValue(undefined) };
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = { url: '/matricula/inscripcion' } as RouterStateSnapshot;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  it('authGuard deja pasar con sesion activa', async () => {
    auth.autenticado.mockReturnValue(true);
    const resultado = await TestBed.runInInjectionContext(() => authGuard(ruta, estado));
    expect(resultado).toBe(true);
    expect(auth.iniciarLogin).not.toHaveBeenCalled();
  });

  it('authGuard sin sesion inicia el login recordando la ruta pedida', async () => {
    auth.autenticado.mockReturnValue(false);
    const resultado = await TestBed.runInInjectionContext(() => authGuard(ruta, estado));
    expect(resultado).toBe(false);
    expect(auth.iniciarLogin).toHaveBeenCalledWith('/matricula/inscripcion');
  });

  it('rolGuard permite el rol exigido', () => {
    auth.tieneRol.mockReturnValue(true);
    const resultado = TestBed.runInInjectionContext(() => rolGuard('ADMIN')(ruta, estado));
    expect(resultado).toBe(true);
    expect(auth.tieneRol).toHaveBeenCalledWith('ADMIN');
  });

  it('rolGuard con rol insuficiente redirige a /sin-permiso (no pide login otra vez)', () => {
    auth.tieneRol.mockReturnValue(false);
    const resultado = TestBed.runInInjectionContext(() => rolGuard('COORDINADOR', 'ADMIN')(ruta, estado));
    expect(resultado instanceof UrlTree).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/sin-permiso');
    expect(auth.iniciarLogin).not.toHaveBeenCalled();
  });
});
