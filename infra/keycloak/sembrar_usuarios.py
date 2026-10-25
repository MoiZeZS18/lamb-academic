"""Crea o actualiza los usuarios de prueba del realm lamb-academic (idempotente).

- Declara en el perfil de usuario el atributo alumnoId (solo el administrador lo ve y edita:
  un estudiante no puede cambiarlo desde la consola de cuenta de Keycloak).
- Cada usuario ESTUDIANTE queda vinculado al ALUMNOS.ID real de Oracle por ese atributo;
  el mapper alumno_id del cliente lamb-frontend lo publica en el token.
- Las claves salen de infra/secretos/keycloak-usuarios.env (generar_secretos.py) y nunca se imprimen.
- Deja el español como idioma predeterminado del realm lamb-academic y de la consola (realm master).

Uso: python -I infra/keycloak/sembrar_usuarios.py
"""
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEYCLOAK = os.environ.get("LAMB_KEYCLOAK_URL", "http://localhost:48080")
REALM = "lamb-academic"

USUARIOS = [
    # username, nombre, apellido, email, rol, alumnoId, clave en keycloak-usuarios.env
    ("ana.quispe", "Ana Lucia", "Quispe Mamani", "ana.quispe@demo.lamb.pe", "ESTUDIANTE", 1001, "ESTUDIANTE_ANA"),
    ("jose.apaza", "Jose Miguel", "Apaza Cruz", "jose.apaza@demo.lamb.pe", "ESTUDIANTE", 1033, "ESTUDIANTE_JOSE"),
    ("coordinador", "Coordinacion", "Academica", "coordinador@demo.lamb.pe", "COORDINADOR", None, "COORDINADOR"),
    ("admin.lamb", "Administracion", "LAMB", "admin@demo.lamb.pe", "ADMIN", None, "ADMIN"),
    ("cajero", "Caja", "Central", "cajero@demo.lamb.pe", "CAJERO", None, "CAJERO"),
]


def leer_env(ruta):
    valores = {}
    for linea in open(ruta, encoding="utf-8"):
        if "=" in linea and not linea.lstrip().startswith("#"):
            k, v = linea.rstrip("\n").split("=", 1)
            valores[k.strip()] = v
    return valores


def pedir(metodo, ruta, token=None, cuerpo=None, form=None):
    datos, cabeceras = None, {}
    if form is not None:
        datos = urllib.parse.urlencode(form).encode()
        cabeceras["Content-Type"] = "application/x-www-form-urlencoded"
    elif cuerpo is not None:
        datos = json.dumps(cuerpo).encode()
        cabeceras["Content-Type"] = "application/json"
    if token:
        cabeceras["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(KEYCLOAK + ruta, data=datos, headers=cabeceras, method=metodo)
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode()
            return r.status, (json.loads(texto) if texto else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def token_admin(infra):
    estado, tok = pedir("POST", "/realms/master/protocol/openid-connect/token", form={
        "grant_type": "password", "client_id": "admin-cli",
        "username": infra.get("KC_ADMIN_USERNAME", "admin"), "password": infra["KC_ADMIN_PASSWORD"]})
    if estado != 200:
        sys.exit(f"No se pudo autenticar como administrador de Keycloak (HTTP {estado})")
    return tok["access_token"]


def main():
    infra = leer_env(os.path.join(RAIZ, ".env"))
    claves = leer_env(os.path.join(RAIZ, "secretos", "keycloak-usuarios.env"))
    token = token_admin(infra)
    base = f"/admin/realms/{REALM}"

    # 0. Español como idioma predeterminado: realm de LAMB (login y cuenta) y realm master
    #    (consola de administración, S10b 3.0). Idempotente.
    for realm in (REALM, "master"):
        _, rep_realm = pedir("GET", f"/admin/realms/{realm}", token)
        if rep_realm.get("defaultLocale") != "es" or not rep_realm.get("internationalizationEnabled"):
            estado, r = pedir("PUT", f"/admin/realms/{realm}", token, {
                "internationalizationEnabled": True,
                "supportedLocales": sorted(set(rep_realm.get("supportedLocales") or []) | {"es"}),
                "defaultLocale": "es"})
            print(f"realm {realm}: idioma español", "configurado" if estado == 204 else f"ERROR {estado} {r}")
        else:
            print(f"realm {realm}: idioma español ya configurado")

    # 1. Atributo alumnoId en el perfil de usuario, editable solo por el administrador.
    _, perfil = pedir("GET", f"{base}/users/profile", token)
    if not any(a["name"] == "alumnoId" for a in perfil["attributes"]):
        perfil["attributes"].append({
            "name": "alumnoId",
            "displayName": "Id de alumno LAMB (ALUMNOS.ID)",
            "permissions": {"view": ["admin"], "edit": ["admin"]},
            "validations": {"integer": {"min": "1"}},
            "multivalued": False,
        })
        estado, r = pedir("PUT", f"{base}/users/profile", token, perfil)
        print("perfil de usuario: atributo alumnoId", "declarado" if estado == 200 else f"ERROR {estado} {r}")
    else:
        print("perfil de usuario: atributo alumnoId ya declarado")

    for username, nombre, apellido, email, rol, alumno_id, clave in USUARIOS:
        token = token_admin(infra)  # el token de admin-cli dura 60 s
        _, encontrados = pedir("GET", f"{base}/users?exact=true&username={urllib.parse.quote(username)}", token)
        representacion = {
            "username": username, "firstName": nombre, "lastName": apellido, "email": email,
            "emailVerified": True, "enabled": True, "requiredActions": [],
            "attributes": {"alumnoId": [str(alumno_id)]} if alumno_id else {},
        }
        if encontrados:
            uid = encontrados[0]["id"]
            estado, r = pedir("PUT", f"{base}/users/{uid}", token, representacion)
            accion = "actualizado"
        else:
            estado, r = pedir("POST", f"{base}/users", token, representacion)
            _, encontrados = pedir("GET", f"{base}/users?exact=true&username={urllib.parse.quote(username)}", token)
            uid = encontrados[0]["id"]
            accion = "creado"
        if estado not in (200, 201, 204):
            sys.exit(f"{username}: ERROR {estado} {r}")
        estado, r = pedir("PUT", f"{base}/users/{uid}/reset-password", token,
                          {"type": "password", "value": claves[f"{clave}_PASSWORD"], "temporary": False})
        if estado != 204:
            sys.exit(f"{username}: no se pudo fijar la clave (HTTP {estado})")
        _, rol_rep = pedir("GET", f"{base}/roles/{rol}", token)
        pedir("POST", f"{base}/users/{uid}/role-mappings/realm", token, [rol_rep])
        print(f"{username}: {accion}, rol {rol}" + (f", alumnoId {alumno_id}" if alumno_id else ""))


if __name__ == "__main__":
    main()
