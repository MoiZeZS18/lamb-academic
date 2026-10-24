"""
Rotacion de credenciales de Oracle (LAMB_APP, PDBADMIN, SYSTEM, SYS) sin exponer valores.

- Genera claves robustas y distintas con `secrets` (24 caracteres, empiezan con letra,
  mayusculas + minusculas + digitos) y las guarda ANTES de aplicarlas en
  infra/secretos/oracle.env (ignorado por Git, permisos solo para el usuario actual).
- Aplica cada ALTER USER como SYSDBA (autenticacion del sistema operativo dentro del contenedor)
  enviando el SQL por la entrada estandar: las claves no aparecen en argumentos, historial ni salida.
- Verifica por red (listener, igual que el backend) que la clave nueva conecta y la antigua no.
- Se detiene ante el primer fallo. El acceso "/ as sysdba" del contenedor no depende de claves.
- Al final actualiza backend/.env (LAMB_DB_PASSWORD) e infra/.env (APP_USER_PASSWORD, ORACLE_PASSWORD).

Uso (desde la raiz del repositorio):  python -I infra/oracle/verificacion/rotar_credenciales.py
"""
import os
import pathlib
import secrets
import string
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parents[3]
CONTENEDOR = "lamb-academic-oracle"
SERVICIO = "//localhost:1521/FREEPDB1"
SECRETOS = RAIZ / "infra" / "secretos" / "oracle.env"
BACKEND_ENV = RAIZ / "backend" / ".env"
INFRA_ENV = RAIZ / "infra" / ".env"

# cuenta -> (contenedor donde se cambia, conexion "AS SYSDBA" para verificar)
CUENTAS = {
    "LAMB_APP": ("FREEPDB1", False),
    "PDBADMIN": ("FREEPDB1", False),
    "SYSTEM": ("CDB$ROOT", False),
    "SYS": ("CDB$ROOT", True),
}


def leer_env(ruta):
    datos = {}
    if ruta.exists():
        for linea in ruta.read_text(encoding="utf-8").splitlines():
            if "=" in linea and not linea.lstrip().startswith("#"):
                k, v = linea.split("=", 1)
                datos[k.strip()] = v.strip()
    return datos


def escribir_env(ruta, valores):
    """Reemplaza o agrega claves conservando el resto del archivo."""
    lineas = ruta.read_text(encoding="utf-8").splitlines() if ruta.exists() else []
    pendientes = dict(valores)
    for i, linea in enumerate(lineas):
        k = linea.split("=", 1)[0].strip()
        if k in pendientes:
            lineas[i] = f"{k}={pendientes.pop(k)}"
    lineas += [f"{k}={v}" for k, v in pendientes.items()]
    ruta.write_text("\n".join(lineas) + "\n", encoding="utf-8")


def proteger(ruta):
    """Solo el usuario actual puede leer o escribir el archivo (ACL de Windows)."""
    usuario = os.environ.get("USERNAME", "")
    r = subprocess.run(["icacls", str(ruta), "/inheritance:r", "/grant:r", f"{usuario}:F"],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise SystemExit(f"No se pudieron restringir los permisos de {ruta.name}")


def nueva_clave():
    alfabeto = string.ascii_letters + string.digits
    while True:
        clave = secrets.choice(string.ascii_letters) + "".join(secrets.choice(alfabeto) for _ in range(23))
        if any(c.islower() for c in clave) and any(c.isupper() for c in clave) and any(c.isdigit() for c in clave):
            return clave


def sqlplus(entrada):
    """Ejecuta SQL*Plus dentro del contenedor; el SQL (con claves) viaja por stdin."""
    r = subprocess.run(["docker", "exec", "-i", CONTENEDOR, "sqlplus", "-s", "/nolog"],
                       input=entrada, capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr


def cambiar(cuenta, contenedor, clave):
    codigo, salida = sqlplus(
        "WHENEVER SQLERROR EXIT 1\nSET ECHO OFF FEEDBACK OFF VERIFY OFF\nCONNECT / AS SYSDBA\n"
        f"ALTER SESSION SET CONTAINER = {contenedor};\n"
        f'ALTER USER {cuenta} IDENTIFIED BY "{clave}";\nEXIT 0\n')
    return codigo == 0, ("ORA-" + salida.split("ORA-", 1)[1][:9]) if "ORA-" in salida else ""


def conecta(cuenta, clave, como_sysdba):
    sufijo = " AS SYSDBA" if como_sysdba else ""
    codigo, salida = sqlplus(
        "WHENEVER SQLERROR EXIT 2\nSET HEADING OFF FEEDBACK OFF\n"
        f'CONNECT {cuenta}/"{clave}"@{SERVICIO}{sufijo}\nSELECT \'CONEXION_OK\' FROM dual;\nEXIT 0\n')
    if "CONEXION_OK" in salida:
        return True, ""
    return False, ("ORA-" + salida.split("ORA-", 1)[1][:9]) if "ORA-" in salida else "sin respuesta"


def main():
    antiguas_backend = leer_env(BACKEND_ENV)
    antiguas_infra = leer_env(INFRA_ENV)
    antiguas = {
        "LAMB_APP": antiguas_backend.get("LAMB_DB_PASSWORD"),
        "PDBADMIN": antiguas_infra.get("ORACLE_PASSWORD"),
        "SYSTEM": antiguas_infra.get("ORACLE_PASSWORD"),
        "SYS": antiguas_infra.get("ORACLE_PASSWORD"),
    }

    # 1. Claves nuevas: se reutilizan si el archivo ya existe (reintento seguro tras un fallo)
    SECRETOS.parent.mkdir(parents=True, exist_ok=True)
    guardadas = leer_env(SECRETOS)
    nuevas = {c: guardadas.get(f"{c}_PASSWORD") or nueva_clave() for c in CUENTAS}
    if len(set(nuevas.values())) != len(nuevas) or any(nuevas[c] == antiguas[c] for c in CUENTAS):
        raise SystemExit("Las claves nuevas deben ser distintas entre si y de las antiguas")
    escribir_env(SECRETOS, {f"{c}_PASSWORD": v for c, v in nuevas.items()})
    proteger(SECRETOS)
    print(f"[1] Claves nuevas guardadas en {SECRETOS.relative_to(RAIZ)} (permisos solo del usuario): "
          + ", ".join(f"{c}={len(v)} caracteres" for c, v in nuevas.items()))

    # 2. Rotar y verificar cuenta por cuenta
    for cuenta, (contenedor, como_sysdba) in CUENTAS.items():
        ok, error = cambiar(cuenta, contenedor, nuevas[cuenta])
        if not ok:
            raise SystemExit(f"[2] {cuenta}: ALTER USER fallo ({error}). Se detiene; el acceso SYSDBA local sigue disponible.")
        nueva_ok, error_nueva = conecta(cuenta, nuevas[cuenta], como_sysdba)
        if not nueva_ok:
            raise SystemExit(f"[2] {cuenta}: la clave nueva NO conecta ({error_nueva}). Se detiene.")
        if antiguas[cuenta]:
            antigua_ok, error_antigua = conecta(cuenta, antiguas[cuenta], como_sysdba)
            estado_antigua = "RECHAZADA " + error_antigua if not antigua_ok else "TODAVIA FUNCIONA"
            if antigua_ok:
                raise SystemExit(f"[2] {cuenta}: la clave antigua todavia conecta. Se detiene.")
        else:
            estado_antigua = "no conocida"
        print(f"[2] {cuenta:<8} rotada en {contenedor:<9} | nueva: CONECTA | antigua: {estado_antigua}")

    # 3. Configuracion local del backend y de Docker
    escribir_env(BACKEND_ENV, {"LAMB_DB_PASSWORD": nuevas["LAMB_APP"]})
    escribir_env(INFRA_ENV, {"APP_USER_PASSWORD": nuevas["LAMB_APP"], "ORACLE_PASSWORD": nuevas["SYSTEM"]})
    proteger(BACKEND_ENV)
    proteger(INFRA_ENV)
    print("[3] backend/.env (LAMB_DB_PASSWORD) e infra/.env (APP_USER_PASSWORD, ORACLE_PASSWORD=SYSTEM) "
          "actualizados y protegidos")


if __name__ == "__main__":
    sys.exit(main())
