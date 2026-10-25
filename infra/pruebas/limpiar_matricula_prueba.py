"""Elimina una matricula creada en una prueba manual de la SPA, para dejar los datos como al inicio.

Uso: python -I infra/pruebas/limpiar_matricula_prueba.py <id_matricula>

Procedimiento controlado (pruebas repetibles sin destruir datos existentes):
  1. En la SPA, la coordinacion ANULA la matricula de prueba: el backend libera los cupos y anula
     el cargo (solo si no tiene pagos).
  2. Este script borra solo esa matricula, sus detalles, su cargo y sus cuotas. Se niega si la
     matricula no esta ANULADA, si su cargo no esta ANULADO o si el cargo tiene pagos.
Mismo patron de limpieza que e2e_repetible.py. La clave de LAMB_APP se lee de backend/.env.
"""
import os
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def clave_bd():
    for linea in open(os.path.join(RAIZ, "backend", ".env"), encoding="utf-8"):
        if linea.startswith("LAMB_DB_PASSWORD="):
            return linea.rstrip("\n").split("=", 1)[1]
    raise SystemExit("Falta LAMB_DB_PASSWORD en backend/.env")


def sqlplus(sql):
    r = subprocess.run(["docker", "exec", "-i", "lamb-academic-oracle", "sqlplus", "-s",
                        f"LAMB_APP/{clave_bd()}@localhost:1521/FREEPDB1"],
                       input="set heading off feedback off pagesize 0\nWHENEVER SQLERROR EXIT FAILURE ROLLBACK\n"
                             + sql + "\nEXIT\n", capture_output=True, text=True)
    if r.returncode != 0:
        raise SystemExit("SQL*Plus fallo: " + r.stdout[-400:])
    return r.stdout.strip()


mid = int(sys.argv[1])
estado = sqlplus(f"SELECT ESTADO FROM MATRICULAS WHERE ID = {mid};")
if estado != "ANULADA":
    sys.exit(f"La matricula {mid} esta '{estado or 'inexistente'}': solo se limpian matriculas ANULADAS desde la SPA.")
cargos = sqlplus(f"SELECT ESTADO || ':' || (SELECT COUNT(*) FROM FIN_PAGOS P WHERE P.ID_CARGO = C.ID) "
                 f"FROM FIN_CARGOS C WHERE PROCESO_ORIGEN = 'MATRICULA' AND ID_REFERENCIA = {mid};").split()
if any(not c.startswith("ANULADO:") or not c.endswith(":0") for c in cargos):
    sys.exit(f"El cargo de la matricula {mid} no esta anulado o tiene pagos ({cargos}); no se toca.")

sqlplus(f"""
DELETE FROM FIN_DETALLE_CARGOS WHERE ID_CARGO IN
  (SELECT ID FROM FIN_CARGOS WHERE PROCESO_ORIGEN = 'MATRICULA' AND ID_REFERENCIA = {mid});
DELETE FROM FIN_CARGOS WHERE PROCESO_ORIGEN = 'MATRICULA' AND ID_REFERENCIA = {mid};
DELETE FROM DETALLE_MATRICULA WHERE MATRICULA_ID = {mid};
DELETE FROM MATRICULAS WHERE ID = {mid} AND ESTADO = 'ANULADA';
COMMIT;
""")
print(f"Matricula de prueba {mid} eliminada con {len(cargos)} cargo(s) anulado(s) y sus cuotas.")
