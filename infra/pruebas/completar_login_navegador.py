"""Completa el formulario de Keycloak para una URL de autorizacion que genero la SPA.

Uso (pruebas automatizadas del navegador, sin escribir claves en ningun registro):
    python -I infra/pruebas/completar_login_navegador.py "<url /auth que abrio la SPA>" ana.quispe

La SPA genero code_verifier y state y los guardo en SU sessionStorage; este script solo envia el
formulario de login (clave leida de infra/secretos/keycloak-usuarios.env) y devuelve la URL de
retorno /auth/callback?code=...&state=... para abrirla en ese mismo navegador. El intercambio del
code por el token (con PKCE) lo hace la SPA, igual que en un login manual.
"""
import os
import sys
import urllib.parse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import keycloak_pkce as kc  # noqa: E402

url, usuario = sys.argv[1], sys.argv[2]
consulta = dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(url).query))
if consulta.get("client_id") != kc.CLIENTE or consulta.get("code_challenge_method") != "S256":
    sys.exit("La URL no es una solicitud PKCE del cliente lamb-frontend")
retorno = kc.obtener_code(usuario, consulta["code_challenge"], consulta["state"])
if "code" not in retorno:
    sys.exit(f"Keycloak no entrego code: {retorno.get('error')}")
print(f"{consulta['redirect_uri']}?{urllib.parse.urlencode(retorno)}")
