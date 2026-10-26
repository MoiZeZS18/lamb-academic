"""Matriz de accesos real (S10b 8 / S11 3.10): tokens de Keycloak obtenidos con PKCE contra el
backend en ejecucion. Solo lectura y operaciones rechazadas: no modifica ningun dato.

Uso: python -I infra/pruebas/matriz_accesos.py
"""
import json
import os
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import keycloak_pkce as kc  # noqa: E402

API = os.environ.get("LAMB_API_URL", "http://localhost:8080")


def llamar(metodo, ruta, token=None, cuerpo=None):
    cabeceras = {"Content-Type": "application/json"}
    if token:
        cabeceras["Authorization"] = f"Bearer {token}"
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, headers=cabeceras, method=metodo)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


tokens = {u: kc.login(u)["access_token"] for u in ["ana.quispe", "jose.apaza", "coordinador", "admin.lamb", "cajero"]}
tokens[None] = None
# Periodo invalido a proposito: si el rol pasa, la validacion responde 400 y nada se guarda.
PERIODO_INVALIDO = {"codigo": "", "nombre": "", "fechaInicio": "2027-03-01", "fechaFin": "2027-07-15", "estado": "X"}
MATRICULA_DE_LUIS = {"matricula": {"estudianteId": 1002, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
                     "detalles": [{"seccionId": 1004}]}

CASOS = [
    # (descripcion, usuario, metodo, ruta, cuerpo, esperado)
    ("Sin token", None, "GET", "/api/v1/periodos", None, 401),
    ("Token alterado", "TOKEN_ALTERADO", "GET", "/api/v1/periodos", None, 401),
    ("Estudiante consulta periodos", "ana.quispe", "GET", "/api/v1/periodos", None, 200),
    ("Estudiante consulta secciones del periodo", "ana.quispe", "GET", "/api/v1/secciones?periodoId=4", None, 200),
    ("Estudiante consulta SU matricula 93", "ana.quispe", "GET", "/api/v1/matriculas/93", None, 200),
    ("Estudiante consulta matricula AJENA 94", "ana.quispe", "GET", "/api/v1/matriculas/94", None, 403),
    ("Estudiante lista matriculas de otro alumno", "ana.quispe", "GET", "/api/v1/matriculas?estudianteId=1002", None, 403),
    ("Estudiante consulta SU cargo 7", "ana.quispe", "GET", "/api/v1/finanzas/cargos/7", None, 200),
    ("Estudiante consulta cargo AJENO 8", "ana.quispe", "GET", "/api/v1/finanzas/cargos/8", None, 403),
    ("Estudiante registra matricula de otro alumno", "jose.apaza", "POST", "/api/v1/matriculas", MATRICULA_DE_LUIS, 403),
    ("Estudiante anula matricula", "ana.quispe", "PATCH", "/api/v1/matriculas/93/anular", None, 403),
    ("Estudiante crea periodo", "ana.quispe", "POST", "/api/v1/periodos", PERIODO_INVALIDO, 403),
    ("Estudiante elimina seccion", "ana.quispe", "DELETE", "/api/v1/secciones/1004", None, 403),
    ("Estudiante consulta personas", "ana.quispe", "GET", "/api/v1/personas", None, 403),
    ("Coordinador consulta matricula 94", "coordinador", "GET", "/api/v1/matriculas/94", None, 200),
    ("Coordinador consulta reporte", "coordinador", "GET", "/api/v1/matriculas/reporte", None, 200),
    ("Coordinador crea periodo", "coordinador", "POST", "/api/v1/periodos", PERIODO_INVALIDO, 403),
    ("Admin crea periodo (pasa el rol; datos invalidos)", "admin.lamb", "POST", "/api/v1/periodos", PERIODO_INVALIDO, 400),
    ("Cajero lista cargos", "cajero", "GET", "/api/v1/finanzas/cargos", None, 200),
    ("Cajero elimina seccion", "cajero", "DELETE", "/api/v1/secciones/1004", None, 403),
    ("Estudiante consulta SU contexto academico", "ana.quispe", "GET", "/api/v1/matriculas/contexto", None, 200),
    ("Estudiante consulta contexto AJENO", "ana.quispe", "GET", "/api/v1/matriculas/contexto?estudianteId=1002", None, 403),
    ("Coordinador consulta contexto de un alumno", "coordinador", "GET", "/api/v1/matriculas/contexto?estudianteId=1033", None, 200),
    ("Coordinador consulta contexto sin alumno", "coordinador", "GET", "/api/v1/matriculas/contexto", None, 400),
    ("Cajero consulta contexto academico", "cajero", "GET", "/api/v1/matriculas/contexto?estudianteId=1033", None, 403),
    ("Perfil del token (estudiante)", "jose.apaza", "GET", "/api/v1/auth/perfil", None, 200),
    ("Health publico", None, "GET", "/actuator/health", None, 200),
]

ok = 0
for descripcion, usuario, metodo, ruta, cuerpo, esperado in CASOS:
    token = tokens["ana.quispe"][:-6] + "AAAAAA" if usuario == "TOKEN_ALTERADO" else tokens[usuario]
    estado, texto = llamar(metodo, ruta, token, cuerpo)
    bien = estado == esperado
    ok += bien
    extra = ""
    if estado in (401, 403):
        try:
            extra = json.loads(texto).get("message", "")
        except ValueError:
            extra = texto[:80]
    print(f"[{'OK' if bien else 'FALLA'}] {estado} (esperado {esperado}) {metodo} {ruta} :: {descripcion}"
          + (f" -> {extra}" if extra else ""))
print(f"\n{ok}/{len(CASOS)} casos de la matriz correctos")
sys.exit(0 if ok == len(CASOS) else 1)
