"""Obtiene tokens reales del realm lamb-academic con Authorization Code + PKCE (S11 3.1, paso 3).

Reproduce lo que hace el navegador: pide la URL de autorizacion con code_challenge y state,
envia el formulario de login que sirve Keycloak, captura el code de la redireccion (sin
seguirla hasta localhost:4200) y lo intercambia con el code_verifier. No usa Direct access
grants: el cliente lamb-frontend lo tiene desactivado.

Las claves de los usuarios de prueba se leen de infra/secretos/keycloak-usuarios.env.
"""
import base64
import hashlib
import html
import http.cookiejar
import json
import os
import re
import secrets
import urllib.error
import urllib.parse
import urllib.request

KEYCLOAK = os.environ.get("LAMB_KEYCLOAK_URL", "http://localhost:48080")
REALM = "lamb-academic"
CLIENTE = "lamb-frontend"
REDIRECT = "http://localhost:4200/auth/callback"
OIDC = f"{KEYCLOAK}/realms/{REALM}/protocol/openid-connect"
SECRETOS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        "secretos", "keycloak-usuarios.env")

# usuario de Keycloak -> clave en keycloak-usuarios.env
CLAVES = {"ana.quispe": "ESTUDIANTE_ANA", "jose.apaza": "ESTUDIANTE_JOSE", "coordinador": "COORDINADOR",
          "admin.lamb": "ADMIN", "cajero": "CAJERO"}


class _SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class _PoliticaLocalhost(http.cookiejar.DefaultCookiePolicy):
    """Igual que un navegador: localhost cuenta como contexto seguro para cookies Secure."""

    def return_ok_secure(self, cookie, request):
        return request.type == "https" or urllib.parse.urlsplit(request.full_url).hostname in ("localhost", "127.0.0.1")


def _b64url(datos):
    return base64.urlsafe_b64encode(datos).rstrip(b"=").decode()


def _clave(usuario):
    for linea in open(SECRETOS, encoding="utf-8"):
        if linea.startswith(CLAVES[usuario] + "_PASSWORD="):
            return linea.rstrip("\n").split("=", 1)[1]
    raise KeyError(usuario)


def url_autorizacion(challenge, state, extra=None):
    parametros = {"client_id": CLIENTE, "response_type": "code", "redirect_uri": REDIRECT,
                  "scope": "openid profile email", "state": state}
    if challenge:
        parametros.update(code_challenge=challenge, code_challenge_method="S256")
    parametros.update(extra or {})
    return f"{OIDC}/auth?{urllib.parse.urlencode(parametros)}"


def obtener_code(usuario, challenge, state):
    """Login en el formulario de Keycloak; devuelve los parametros de la redireccion final."""
    navegador = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar(_PoliticaLocalhost())), _SinRedireccion())
    try:
        with navegador.open(url_autorizacion(challenge, state)) as r:
            pagina = r.read().decode()
    except urllib.error.HTTPError as e:
        # Keycloak rechaza la solicitud antes del login (p. ej. falta code_challenge).
        if e.code not in (302, 303):
            raise
        return dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(e.headers["Location"]).query))
    accion = re.search(r'<form[^>]+id="kc-form-login"[^>]+action="([^"]+)"', pagina)
    if not accion:
        raise RuntimeError("Keycloak no mostro el formulario de login")
    formulario = urllib.parse.urlencode({"username": usuario, "password": _clave(usuario),
                                         "credentialId": ""}).encode()
    try:
        navegador.open(html.unescape(accion.group(1)), formulario)
        raise RuntimeError("Login rechazado: Keycloak no redirigio a la SPA")
    except urllib.error.HTTPError as e:
        if e.code not in (302, 303):
            raise
        destino = e.headers["Location"]
    return dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(destino).query))


def intercambiar(datos):
    req = urllib.request.Request(f"{OIDC}/token", data=urllib.parse.urlencode(datos).encode(),
                                 headers={"Content-Type": "application/x-www-form-urlencoded"})
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}")


def login(usuario):
    """Flujo completo: devuelve la respuesta del endpoint de token (access, refresh, id token)."""
    verifier = _b64url(secrets.token_bytes(32))
    challenge = _b64url(hashlib.sha256(verifier.encode()).digest())
    state = _b64url(secrets.token_bytes(16))
    retorno = obtener_code(usuario, challenge, state)
    if retorno.get("state") != state or "code" not in retorno:
        raise RuntimeError(f"Redireccion inesperada: {sorted(retorno)}")
    estado, tokens = intercambiar({"grant_type": "authorization_code", "client_id": CLIENTE,
                                   "redirect_uri": REDIRECT, "code": retorno["code"], "code_verifier": verifier})
    if estado != 200:
        raise RuntimeError(f"Intercambio rechazado: HTTP {estado} {tokens.get('error')}")
    return tokens


def refrescar(refresh_token):
    return intercambiar({"grant_type": "refresh_token", "client_id": CLIENTE, "refresh_token": refresh_token})


def claims(token):
    carga = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(carga + "=" * (-len(carga) % 4)))
