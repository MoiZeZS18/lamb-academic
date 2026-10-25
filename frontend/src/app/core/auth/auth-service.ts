import { Injectable, computed, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { decodeJwtPayload } from './jwt';
import { generarCodeChallenge, generarCodeVerifier, generarState } from './pkce';

interface SesionKeycloak {
  accessToken: string;
  refreshToken: string;
  idToken: string;
  expiraEn: number;
}

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  id_token: string;
  expires_in: number;
}

const CLAVE_CODE_VERIFIER = 'lamb_pkce_code_verifier';
const CLAVE_STATE = 'lamb_pkce_state';
const CLAVE_DESTINO = 'lamb_pkce_destino';
const CLAVE_SESION = 'lamb_sesion';

/**
 * Autenticacion contra Keycloak con Authorization Code + PKCE (S11 3.3).
 *
 * La sesion vive en un signal en memoria, con una copia espejo en sessionStorage para sobrevivir
 * un F5 en la misma pestana (S11 2.3). Nunca se usa localStorage. La SPA nunca ve una contrasena:
 * el login lo sirve Keycloak. Diferencia con la guia: refrescar() comparte una sola promesa entre
 * peticiones simultaneas, para que varios 401 a la vez no disparen varios refrescos.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sesion = signal<SesionKeycloak | null>(this.restaurarSesion());
  private refrescoEnCurso: Promise<boolean> | null = null;

  readonly autenticado = computed(() => {
    const actual = this.sesion();
    return actual !== null && actual.expiraEn > Date.now();
  });

  private readonly payload = computed(() => {
    const token = this.sesion()?.accessToken;
    return token ? decodeJwtPayload(token) : null;
  });

  readonly roles = computed<string[]>(() => this.payload()?.realm_access?.roles ?? []);

  /** ALUMNOS.ID vinculado al usuario (solo ESTUDIANTE); el backend lo vuelve a leer del token. */
  readonly alumnoId = computed<number | null>(() => this.payload()?.alumno_id ?? null);

  readonly usuario = computed(() => {
    const payload = this.payload();
    if (!payload) return null;
    return {
      nombre: payload.name ?? payload.preferred_username ?? 'Usuario',
      email: payload.email ?? '',
      usuario: payload.preferred_username ?? '',
    };
  });

  tieneRol(...rolesPermitidos: string[]): boolean {
    return rolesPermitidos.some((rol) => this.roles().includes(rol));
  }

  accessToken(): string | null {
    return this.sesion()?.accessToken ?? null;
  }

  async iniciarLogin(destino = location.pathname + location.search): Promise<void> {
    const verifier = generarCodeVerifier();
    const state = generarState();
    const challenge = await generarCodeChallenge(verifier);
    sessionStorage.setItem(CLAVE_CODE_VERIFIER, verifier);
    sessionStorage.setItem(CLAVE_STATE, state);
    sessionStorage.setItem(CLAVE_DESTINO, destino);
    const parametros = new URLSearchParams({
      client_id: environment.keycloak.clientId,
      response_type: 'code',
      redirect_uri: environment.keycloak.redirectUri,
      scope: 'openid profile email',
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });
    this.navegar(`${environment.keycloak.issuer}/protocol/openid-connect/auth?${parametros}`);
  }

  async completarLogin(code: string, stateRecibido: string): Promise<void> {
    const stateEsperado = sessionStorage.getItem(CLAVE_STATE);
    const verifier = sessionStorage.getItem(CLAVE_CODE_VERIFIER);
    sessionStorage.removeItem(CLAVE_STATE);
    if (!verifier || stateRecibido !== stateEsperado) {
      sessionStorage.removeItem(CLAVE_CODE_VERIFIER);
      throw new Error(
        'La respuesta de Keycloak no corresponde a esta solicitud de inicio de sesión (posible ataque CSRF). Inicia sesión de nuevo.',
      );
    }
    const cuerpo = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: environment.keycloak.clientId,
      redirect_uri: environment.keycloak.redirectUri,
      code,
      code_verifier: verifier,
    });
    // fetch y no HttpClient: este intercambio no debe pasar por authInterceptor (S11 3.3).
    const respuesta = await fetch(`${environment.keycloak.issuer}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: cuerpo,
    });
    sessionStorage.removeItem(CLAVE_CODE_VERIFIER);
    if (!respuesta.ok) {
      throw new Error('Keycloak no aceptó el código de autorización. Inicia sesión de nuevo.');
    }
    this.guardarSesion(await respuesta.json());
  }

  destinoPendiente(): string {
    const destino = sessionStorage.getItem(CLAVE_DESTINO) ?? '/';
    sessionStorage.removeItem(CLAVE_DESTINO);
    return destino.startsWith('/') && !destino.startsWith('/auth/') ? destino : '/';
  }

  refrescar(): Promise<boolean> {
    this.refrescoEnCurso ??= this.pedirRefresco().finally(() => (this.refrescoEnCurso = null));
    return this.refrescoEnCurso;
  }

  cerrarSesion(): void {
    const idToken = this.sesion()?.idToken;
    this.sesion.set(null);
    sessionStorage.removeItem(CLAVE_SESION);
    const parametros = new URLSearchParams({
      client_id: environment.keycloak.clientId,
      post_logout_redirect_uri: environment.keycloak.postLogoutRedirectUri,
      ...(idToken ? { id_token_hint: idToken } : {}),
    });
    this.navegar(`${environment.keycloak.issuer}/protocol/openid-connect/logout?${parametros}`);
  }

  /** Redireccion completa del navegador (aislada para poder probar el servicio). */
  protected navegar(url: string): void {
    location.href = url;
  }

  private async pedirRefresco(): Promise<boolean> {
    const refreshToken = this.sesion()?.refreshToken;
    if (!refreshToken) return false;
    const cuerpo = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: environment.keycloak.clientId,
      refresh_token: refreshToken,
    });
    try {
      const respuesta = await fetch(`${environment.keycloak.issuer}/protocol/openid-connect/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: cuerpo,
      });
      if (respuesta.ok) {
        this.guardarSesion(await respuesta.json());
        return true;
      }
    } catch {
      // Keycloak inaccesible: se trata igual que un refresh_token vencido.
    }
    this.sesion.set(null);
    sessionStorage.removeItem(CLAVE_SESION);
    return false;
  }

  private guardarSesion(datos: TokenResponse): void {
    const sesion: SesionKeycloak = {
      accessToken: datos.access_token,
      refreshToken: datos.refresh_token,
      idToken: datos.id_token,
      expiraEn: Date.now() + datos.expires_in * 1000,
    };
    this.sesion.set(sesion);
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
  }

  private restaurarSesion(): SesionKeycloak | null {
    const guardada = sessionStorage.getItem(CLAVE_SESION);
    if (!guardada) return null;
    try {
      const sesion = JSON.parse(guardada) as SesionKeycloak;
      if (sesion.expiraEn > Date.now()) {
        return sesion;
      }
    } catch {
      // JSON invalido: se descarta igual que una sesion vencida
    }
    sessionStorage.removeItem(CLAVE_SESION);
    return null;
  }
}
