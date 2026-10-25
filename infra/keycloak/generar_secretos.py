"""Genera (una sola vez) las claves locales de Keycloak sin mostrarlas.

- KC_ADMIN_PASSWORD y KC_DB_PASSWORD se agregan a infra/.env si faltan.
- Las claves de los usuarios de prueba del realm van a infra/secretos/keycloak-usuarios.env.
Ambos archivos estan ignorados por Git y quedan con acceso solo para el usuario actual.
Si una clave ya existe, se conserva: el script es idempotente.

Uso: python -I infra/keycloak/generar_secretos.py
"""
import getpass
import os
import secrets
import string
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_INFRA = os.path.join(RAIZ, ".env")
ENV_USUARIOS = os.path.join(RAIZ, "secretos", "keycloak-usuarios.env")

# Usuarios de prueba del realm (claves locales, nunca en Git).
USUARIOS = ["ESTUDIANTE_ANA", "ESTUDIANTE_JOSE", "COORDINADOR", "ADMIN", "CAJERO"]


def generar(longitud=20):
    alfabeto = string.ascii_letters + string.digits
    while True:
        clave = secrets.choice(string.ascii_letters) + "".join(secrets.choice(alfabeto) for _ in range(longitud - 1))
        if any(c.islower() for c in clave) and any(c.isupper() for c in clave) and any(c.isdigit() for c in clave):
            return clave


def leer(ruta):
    valores = {}
    if os.path.exists(ruta):
        for linea in open(ruta, encoding="utf-8"):
            if "=" in linea and not linea.lstrip().startswith("#"):
                k, v = linea.rstrip("\n").split("=", 1)
                valores[k.strip()] = v
    return valores


def agregar(ruta, claves):
    actuales = leer(ruta)
    nuevas = {k: generar() for k in claves if not actuales.get(k)}
    if nuevas:
        existe = os.path.exists(ruta)
        with open(ruta, "a", encoding="utf-8") as f:
            if existe and not open(ruta, encoding="utf-8").read().endswith("\n"):
                f.write("\n")
            for k, v in nuevas.items():
                f.write(f"{k}={v}\n")
    proteger(ruta)
    return sorted(nuevas)


def proteger(ruta):
    if os.name == "nt":
        subprocess.run(["icacls", ruta, "/inheritance:r", "/grant:r", f"{getpass.getuser()}:F"],
                       check=True, capture_output=True)


if __name__ == "__main__":
    os.makedirs(os.path.dirname(ENV_USUARIOS), exist_ok=True)
    infra = agregar(ENV_INFRA, ["KC_ADMIN_PASSWORD", "KC_DB_PASSWORD"])
    usuarios = agregar(ENV_USUARIOS, [f"{u}_PASSWORD" for u in USUARIOS])
    print("infra/.env, claves nuevas:", infra or "ninguna (ya existian)")
    print("secretos/keycloak-usuarios.env, claves nuevas:", usuarios or "ninguna (ya existian)")
    sys.exit(0)
