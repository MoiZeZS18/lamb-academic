/**
 * Lee los claims de un JWT SIN verificar su firma (S11 3.5): la SPA nunca valida un token,
 * eso lo hace el backend. Solo decide que mostrar a partir de lo que Keycloak ya emitio.
 */
export interface JwtPayload {
  sub: string;
  exp: number;
  name?: string;
  email?: string;
  preferred_username?: string;
  realm_access?: { roles: string[] };
  /** ALUMNOS.ID del estudiante (mapper alumno_id del cliente lamb-frontend). */
  alumno_id?: number;
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  const partes = token.split('.');
  if (partes.length !== 3) return null;
  try {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((caracter) => '%' + caracter.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
