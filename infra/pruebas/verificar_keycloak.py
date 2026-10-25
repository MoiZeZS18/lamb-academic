"""Verificacion del cliente lamb-frontend (S11 3.1): sin Direct access grants, PKCE obligatorio,
flujo real con PKCE, claims del token y refresco. No imprime tokens ni claves.

Uso: python -I infra/pruebas/verificar_keycloak.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import keycloak_pkce as kc  # noqa: E402

resultados = []


def caso(nombre, ok, detalle=""):
    resultados.append(ok)
    print(f"[{'OK' if ok else 'FALLA'}] {nombre}" + (f" -> {detalle}" if detalle else ""))


# 1. El atajo de S10b (password grant) ya no funciona.
estado, r = kc.intercambiar({"grant_type": "password", "client_id": kc.CLIENTE,
                             "username": "ana.quispe", "password": kc._clave("ana.quispe")})
caso("Direct access grants rechazado", estado == 400 and r.get("error") == "unauthorized_client",
     f"HTTP {estado} {r.get('error')}")

# 2. Sin code_challenge, Keycloak no entrega code (Require PKCE = S256).
retorno = kc.obtener_code("ana.quispe", None, "estado-prueba")
caso("PKCE obligatorio (sin code_challenge no hay code)", "code" not in retorno and retorno.get("error") == "invalid_request",
     f"error={retorno.get('error')} ({retorno.get('error_description', '')})")

# 3. Flujo completo con PKCE para cada usuario, y claims esperados.
esperados = {"ana.quispe": ("ESTUDIANTE", 1001), "jose.apaza": ("ESTUDIANTE", 1033),
             "coordinador": ("COORDINADOR", None), "admin.lamb": ("ADMIN", None), "cajero": ("CAJERO", None)}
for usuario, (rol, alumno) in esperados.items():
    tokens = kc.login(usuario)
    c = kc.claims(tokens["access_token"])
    roles = c.get("realm_access", {}).get("roles", [])
    caso(f"Login PKCE {usuario}", rol in roles and c.get("alumno_id") == alumno and c.get("azp") == kc.CLIENTE,
         f"rol={rol in roles} alumno_id={c.get('alumno_id')} expira_en={tokens['expires_in']}s")

# 4. Refresco: el refresh_token entrega un access_token nuevo sin volver a pedir clave.
tokens = kc.login("coordinador")
estado, nuevo = kc.refrescar(tokens["refresh_token"])
caso("Refresco con refresh_token", estado == 200 and nuevo["access_token"] != tokens["access_token"],
     f"HTTP {estado}")

print(f"\n{sum(resultados)}/{len(resultados)} verificaciones correctas")
sys.exit(0 if all(resultados) else 1)
