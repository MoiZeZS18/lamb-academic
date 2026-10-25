/**
 * Sesiones de prueba con la misma forma que los tokens de Keycloak (realm_access.roles,
 * alumno_id, name). La firma es falsa: la SPA nunca valida firmas, eso lo hace el backend.
 */
function base64Url(objeto: object): string {
  return btoa(unescape(encodeURIComponent(JSON.stringify(objeto))))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

export function tokenDePrueba(roles: string[], extra: Record<string, unknown> = {}): string {
  const payload = {
    sub: 'sub-prueba',
    exp: Math.floor(Date.now() / 1000) + 300,
    name: 'Usuario Prueba',
    preferred_username: 'usuario.prueba',
    realm_access: { roles: [...roles, 'offline_access', 'uma_authorization'] },
    ...extra,
  };
  return `${base64Url({ alg: 'RS256', typ: 'JWT' })}.${base64Url(payload)}.firma`;
}

/** Deja una sesion vigente en sessionStorage para que AuthService la restaure al construirse. */
export function iniciarSesionDePrueba(roles: string[], alumnoId?: number): void {
  const accessToken = tokenDePrueba(roles, alumnoId ? { alumno_id: alumnoId } : {});
  sessionStorage.setItem(
    'lamb_sesion',
    JSON.stringify({ accessToken, refreshToken: 'refresh-prueba', idToken: 'id-prueba', expiraEn: Date.now() + 300_000 }),
  );
}

export function cerrarSesionDePrueba(): void {
  sessionStorage.clear();
}
