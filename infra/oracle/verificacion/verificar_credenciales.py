"""
Verifica (solo lectura) que las claves vigentes de LAMB_APP, PDBADMIN, SYSTEM y SYS conectan por el
listener y que la clave comprometida del commit 4e5beab es rechazada. No imprime valores.

Uso (desde la raiz del repositorio):  python -I infra/oracle/verificacion/verificar_credenciales.py
"""
import pathlib
import re
import subprocess

RAIZ = pathlib.Path(__file__).resolve().parents[3]
SERVICIO = "//localhost:1521/FREEPDB1"
CUENTAS = {"LAMB_APP": False, "PDBADMIN": False, "SYSTEM": False, "SYS": True}


def leer_env(ruta):
    return dict(l.split("=", 1) for l in ruta.read_text(encoding="utf-8").splitlines() if "=" in l)


def clave_comprometida():
    texto = subprocess.run(["git", "-C", str(RAIZ), "show", "4e5beab:backend/src/main/resources/application-dev.yml"],
                           capture_output=True, text=True).stdout
    m = re.search(r"^\s*password:\s*(\S+)", texto, re.M)
    return m.group(1) if m else None


def conecta(cuenta, clave, sysdba):
    entrada = ("SET HEADING OFF FEEDBACK OFF\n"
               f'CONNECT {cuenta}/"{clave}"@{SERVICIO}{" AS SYSDBA" if sysdba else ""}\n'
               "SELECT 'CONEXION_OK' FROM dual;\nEXIT\n")
    r = subprocess.run(["docker", "exec", "-i", "lamb-academic-oracle", "sqlplus", "-s", "/nolog"],
                       input=entrada, capture_output=True, text=True)
    if "CONEXION_OK" in r.stdout:
        return "CONECTA"
    error = re.search(r"ORA-\d{5}", r.stdout)
    return "RECHAZADA " + (error.group(0) if error else "sin respuesta")


def main():
    vigentes = leer_env(RAIZ / "infra" / "secretos" / "oracle.env")
    backend = leer_env(RAIZ / "backend" / ".env").get("LAMB_DB_PASSWORD")
    antigua = clave_comprometida()
    print(f"backend/.env usa la clave vigente de LAMB_APP: {'SI' if backend == vigentes['LAMB_APP_PASSWORD'] else 'NO'}")
    for cuenta, sysdba in CUENTAS.items():
        nueva = conecta(cuenta, vigentes[f"{cuenta}_PASSWORD"], sysdba)
        vieja = conecta(cuenta, antigua, sysdba) if antigua else "no disponible"
        print(f"{cuenta:<8} clave vigente: {nueva:<22} clave comprometida: {vieja}")


if __name__ == "__main__":
    main()
