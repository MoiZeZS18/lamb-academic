import { TestBed } from '@angular/core/testing';
import { cerrarSesionDePrueba, iniciarSesionDePrueba, tokenDePrueba } from '../../../testing/sesion-prueba';
import { AuthService } from './auth-service';

function respuestaToken(roles: string[]) {
  return new Response(
    JSON.stringify({ access_token: tokenDePrueba(roles), refresh_token: 'r2', id_token: 'i2', expires_in: 300 }),
    { status: 200 },
  );
}

describe('AuthService', () => {
  let navegaciones: string[];

  function crear(): AuthService {
    const servicio = TestBed.inject(AuthService);
    vi.spyOn(servicio as unknown as { navegar: (u: string) => void }, 'navegar').mockImplementation((url) =>
      navegaciones.push(url),
    );
    return servicio;
  }

  beforeEach(() => {
    navegaciones = [];
    cerrarSesionDePrueba();
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  afterEach(() => vi.restoreAllMocks());

  it('restaura la sesion del espejo en sessionStorage (sobrevive F5) con roles y alumno', () => {
    iniciarSesionDePrueba(['ESTUDIANTE'], 1033);
    const auth = crear();
    expect(auth.autenticado()).toBe(true);
    expect(auth.tieneRol('ESTUDIANTE')).toBe(true);
    expect(auth.tieneRol('ADMIN', 'COORDINADOR')).toBe(false);
    expect(auth.alumnoId()).toBe(1033);
    expect(auth.usuario()?.nombre).toBe('Usuario Prueba');
  });

  it('descarta una sesion vencida', () => {
    sessionStorage.setItem('lamb_sesion', JSON.stringify({ accessToken: 'x', refreshToken: 'y', idToken: 'z', expiraEn: 1 }));
    const auth = crear();
    expect(auth.autenticado()).toBe(false);
    expect(sessionStorage.getItem('lamb_sesion')).toBeNull();
  });

  it('iniciarLogin redirige a Keycloak con PKCE S256 y state, guardando solo verifier/state/destino', async () => {
    const auth = crear();
    await auth.iniciarLogin('/matricula/inscripcion');
    const url = new URL(navegaciones[0]);
    expect(url.pathname).toBe('/realms/lamb-academic/protocol/openid-connect/auth');
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('client_id')).toBe('lamb-frontend');
    expect(url.searchParams.get('state')).toBe(sessionStorage.getItem('lamb_pkce_state'));
    expect(sessionStorage.getItem('lamb_pkce_destino')).toBe('/matricula/inscripcion');
    expect(localStorage.length).toBe(0);
  });

  it('completarLogin rechaza un state distinto sin llamar al endpoint de token (anti-CSRF)', async () => {
    const auth = crear();
    await auth.iniciarLogin('/');
    const fetchSpy = vi.spyOn(globalThis, 'fetch');
    await expect(auth.completarLogin('codigo', 'state-de-un-atacante')).rejects.toThrow(/CSRF/);
    expect(fetchSpy).not.toHaveBeenCalled();
    expect(auth.autenticado()).toBe(false);
  });

  it('completarLogin intercambia el code con el code_verifier y guarda la sesion en memoria y sessionStorage', async () => {
    const auth = crear();
    await auth.iniciarLogin('/matricula');
    const state = sessionStorage.getItem('lamb_pkce_state')!;
    const verifier = sessionStorage.getItem('lamb_pkce_code_verifier')!;
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(respuestaToken(['COORDINADOR']));

    await auth.completarLogin('codigo-123', state);

    const cuerpo = new URLSearchParams(String(fetchSpy.mock.calls[0][1]?.body));
    expect(cuerpo.get('grant_type')).toBe('authorization_code');
    expect(cuerpo.get('code_verifier')).toBe(verifier);
    expect(auth.tieneRol('COORDINADOR')).toBe(true);
    expect(sessionStorage.getItem('lamb_pkce_code_verifier')).toBeNull();
    expect(sessionStorage.getItem('lamb_sesion')).not.toBeNull();
    expect(localStorage.length).toBe(0);
    expect(auth.destinoPendiente()).toBe('/matricula');
  });

  it('varios refrescos simultaneos comparten una sola llamada a Keycloak', async () => {
    iniciarSesionDePrueba(['ESTUDIANTE'], 1001);
    const auth = crear();
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(respuestaToken(['ESTUDIANTE']));
    const resultados = await Promise.all([auth.refrescar(), auth.refrescar(), auth.refrescar()]);
    expect(resultados).toEqual([true, true, true]);
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });

  it('si el refresh_token ya no sirve, limpia la sesion', async () => {
    iniciarSesionDePrueba(['ESTUDIANTE'], 1001);
    const auth = crear();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 400 }));
    expect(await auth.refrescar()).toBe(false);
    expect(auth.autenticado()).toBe(false);
    expect(sessionStorage.getItem('lamb_sesion')).toBeNull();
  });

  it('cerrarSesion borra la sesion y hace RP-Initiated Logout con id_token_hint', () => {
    iniciarSesionDePrueba(['ADMIN']);
    const auth = crear();
    auth.cerrarSesion();
    const url = new URL(navegaciones[0]);
    expect(url.pathname).toBe('/realms/lamb-academic/protocol/openid-connect/logout');
    expect(url.searchParams.get('id_token_hint')).toBe('id-prueba');
    expect(auth.autenticado()).toBe(false);
    expect(sessionStorage.getItem('lamb_sesion')).toBeNull();
  });
});
