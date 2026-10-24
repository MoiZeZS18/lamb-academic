"""HISTORICO (2026-10-24, primera integracion): 46 casos que CREAN datos fijos de demostracion.
NO es repetible (una segunda ejecucion devuelve 409 donde esperaba 201). Se conserva como evidencia.
Para volver a probar usar infra/pruebas/e2e_repetible.py (datos aislados y autolimpieza)."""
import json
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:8080"
filas = []
fallos = 0


def llamar(metodo, ruta, cuerpo=None, headers=None):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode("utf-8")
            try:
                cuerpo_resp = json.loads(texto) if texto else None
            except ValueError:
                cuerpo_resp = texto
            return r.status, cuerpo_resp, dict(r.headers)
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8")
        try:
            return e.code, json.loads(texto), dict(e.headers)
        except ValueError:
            return e.code, texto, dict(e.headers)


def caso(nombre, metodo, ruta, esperado, cuerpo=None, headers=None, verificar=None):
    global fallos
    estado, body, hdrs = llamar(metodo, ruta, cuerpo, headers)
    ok = estado == esperado
    detalle = ""
    if ok and verificar:
        try:
            detalle = verificar(body, hdrs) or ""
        except AssertionError as ex:
            ok = False
            detalle = "verificacion fallida: " + str(ex)
    if not ok and not detalle:
        detalle = (body.get("message") if isinstance(body, dict) else str(body))[:120]
    if not ok:
        fallos += 1
    filas.append((nombre, f"{metodo} {ruta}", esperado, estado, "OK" if ok else "FALLA", detalle))
    return body


def cupos(seccion_id):
    return llamar("GET", f"/api/v1/secciones/{seccion_id}")[1]["cuposOcupados"]


# --- Verificacion y documentacion ---
caso("Salud con Oracle", "GET", "/actuator/health", 200,
     verificar=lambda b, h: f"status={b['status']} db={b['components']['db']['status']}")
caso("OpenAPI publicado", "GET", "/v3/api-docs", 200,
     verificar=lambda b, h: f"{len(b['paths'])} rutas documentadas")
caso("Endpoint de verificacion", "GET", "/api/v1/hello", 200)
caso("Modulos Modulith (actuator)", "GET", "/actuator/modulith", 200,
     verificar=lambda b, h: "modulos=" + ",".join(sorted(b.keys())))

# --- Base Universal ---
caso("Arbol institucional", "GET", "/api/v1/institucional/arbol", 200,
     verificar=lambda b, h: f"sedes={len(b)}")
p = caso("Crear persona", "POST", "/api/v1/personas", 201, {
    "tipoPersona": "ALUMNO", "tipoDocumento": "DNI", "numeroDocumento": "70000004",
    "nombres": "Jose Miguel", "apellidoPaterno": "Apaza", "apellidoMaterno": "Cruz",
    "email": "jose.apaza@demo.lamb.pe", "idEscuela": 1})
caso("Persona con DNI invalido (400)", "POST", "/api/v1/personas", 400, {
    "tipoPersona": "ALUMNO", "tipoDocumento": "DNI", "numeroDocumento": "12AB",
    "nombres": "X", "apellidoPaterno": "Y"},
     verificar=lambda b, h: "errores=" + ",".join(b["errores"].keys()))
caso("Persona con documento duplicado (409)", "POST", "/api/v1/personas", 409, {
    "tipoPersona": "ALUMNO", "tipoDocumento": "DNI", "numeroDocumento": "70000004",
    "nombres": "Jose", "apellidoPaterno": "Apaza"})
caso("Persona inexistente (404)", "GET", "/api/v1/personas/999999", 404)
caso("Filtro combinado de personas", "GET", "/api/v1/personas?tipoPersona=ALUMNO&q=apaza&sortBy=nombres&dir=asc", 200,
     verificar=lambda b, h: f"resultados={len(b)}")
a = caso("Registrar alumno", "POST", "/api/v1/alumnos", 201, {
    "personaId": p["id"], "codigoEstudiante": "2026100004", "fechaIngreso": "2025-03-01"})
caso("Docente como alumno (409)", "POST", "/api/v1/alumnos", 409, {
    "personaId": 4, "codigoEstudiante": "2026199999", "fechaIngreso": "2025-03-01"})
e = caso("Expediente cabecera-detalle", "POST", "/api/v1/expedientes", 201, {
    "codigoTramite": "EXP-2026-0001", "asunto": "Constancia de matricula", "personaId": p["id"],
    "documentos": [
        {"nombre": "DNI", "nombreArchivo": "dni.pdf", "tipoDocumento": "PDF", "rutaArchivo": "/docs/dni.pdf"},
        {"nombre": "Foto", "nombreArchivo": "foto.jpg", "tipoDocumento": "JPG", "rutaArchivo": "/docs/foto.jpg"}]},
     verificar=lambda b, h: f"totalDocumentos={b['totalDocumentos']}")
caso("Expediente con archivo inconsistente: rollback (409)", "POST", "/api/v1/expedientes", 409, {
    "codigoTramite": "EXP-2026-0002", "asunto": "Rollback", "personaId": p["id"],
    "documentos": [
        {"nombre": "Ok", "nombreArchivo": "ok.pdf", "tipoDocumento": "PDF", "rutaArchivo": "/d/ok.pdf"},
        {"nombre": "Mal", "nombreArchivo": "mal.png", "tipoDocumento": "PDF", "rutaArchivo": "/d/mal.png"}]})
caso("Rollback verificado: EXP-2026-0002 no existe", "GET", "/api/v1/expedientes?personaId=%d" % p["id"], 200,
     verificar=lambda b, h: (lambda c: (_ for _ in ()).throw(AssertionError(c)) if "EXP-2026-0002" in c else f"codigos={c}")(
         [x["codigoTramite"] for x in b]))
caso("Resumen de expedientes", "GET", "/api/v1/expedientes/resumen", 200)

# --- Core Academico ---
caso("Cursos filtrados", "GET", "/api/v1/cursos?q=IS1&sortBy=creditos&dir=desc", 200,
     verificar=lambda b, h: f"cursos={len(b)}")
caso("Malla vigente con prerrequisitos", "GET", "/api/v1/planes-estudio?estado=VIGENTE", 200,
     verificar=lambda b, h: f"planes={len(b)} creditos={b[0]['totalCreditos']}")
caso("Prerrequisitos de IS201", "GET", "/api/v1/cursos/5/prerrequisitos", 200,
     verificar=lambda b, h: "prerrequisitos=" + ",".join(c["codigo"] for c in b))
caso("Seccion con aula ocupada (409)", "POST", "/api/v1/secciones", 409, {
    "cursoId": 6, "periodoId": 4, "grupo": "B", "limiteVacantes": 20,
    "horarios": [{"diaSemana": "LUNES", "horaInicio": "09:00", "horaFin": "10:00", "aula": "A-101"}]})
caso("Seccion con docente ocupado (409)", "POST", "/api/v1/secciones", 409, {
    "cursoId": 6, "periodoId": 4, "grupo": "C", "docenteId": 4, "limiteVacantes": 20,
    "horarios": [{"diaSemana": "LUNES", "horaInicio": "08:00", "horaFin": "09:00", "aula": "A-999"}]})
caso("Secciones con vacantes del periodo", "GET", "/api/v1/secciones?periodoId=4&conVacantes=true", 200,
     verificar=lambda b, h: f"secciones={len(b)}")
caso("Carga docente", "GET", "/api/v1/docentes/4/carga?periodoId=4", 200,
     verificar=lambda b, h: f"secciones={b['totalSecciones']} horas={b['horasSemanales']}")

# --- Matricula + Finanzas ---
antes_102 = cupos(1002)
m1 = caso("Matricula Ana (IS101, IS102, IS103) con cargo", "POST", "/api/v1/matriculas", 201, {
    "matricula": {"estudianteId": 1001, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1001}, {"seccionId": 1002}, {"seccionId": 1003}]},
     headers={"X-Trace-ID": "demo-matricula-ana"},
     verificar=lambda b, h: f"creditos={b['totalCreditos']} cargo={b['cargo']['numeroCargo']} monto={b['cargo']['montoTotal']} traceId={h.get('X-Trace-ID')}")
caso("Matricula duplicada (409)", "POST", "/api/v1/matriculas", 409, {
    "matricula": {"estudianteId": 1001, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1006}]})
caso("Cruce de horario IS101/IS104 (409)", "POST", "/api/v1/matriculas", 409, {
    "matricula": {"estudianteId": 1003, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1001}, {"seccionId": 1004}]})
caso("Prerrequisito no cumplido: Rosa en IS201 (409)", "POST", "/api/v1/matriculas", 409, {
    "matricula": {"estudianteId": 1003, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1005}]})
caso("Prerrequisito cumplido: Luis en IS201 + IS103", "POST", "/api/v1/matriculas", 201, {
    "matricula": {"estudianteId": 1002, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA", "numeroCuotas": 3},
    "detalles": [{"seccionId": 1005}, {"seccionId": 1003}]},
     verificar=lambda b, h: f"cargo={b['cargo']['montoTotal']} en {b['cargo']['numeroCuotas']} cuotas")
caso("IS103 ya llena (2/2): Rosa con IS102 + IS103 (409)", "POST", "/api/v1/matriculas", 409, {
    "matricula": {"estudianteId": 1003, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1002}, {"seccionId": 1003}]})
caso("Rollback: IS102 no quedo reservada para Rosa", "GET", "/api/v1/secciones/1002", 200,
     verificar=lambda b, h: (lambda c: f"cupos IS102={c} (antes {antes_102}+1 de Ana)" if c == antes_102 + 1
                             else (_ for _ in ()).throw(AssertionError(c)))(b["cuposOcupados"]))
caso("Fecha fuera del periodo (409)", "POST", "/api/v1/matriculas", 409, {
    "matricula": {"estudianteId": 1003, "periodoId": 4, "fecha": "2027-01-10", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1006}]})
caso("Matricula sin detalles (400)", "POST", "/api/v1/matriculas", 400, {
    "matricula": {"estudianteId": 1003, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": []})
caso("Estudiante inexistente (404)", "POST", "/api/v1/matriculas", 404, {
    "matricula": {"estudianteId": 9999, "periodoId": 4, "fecha": "2026-10-24", "estado": "INICIADA"},
    "detalles": [{"seccionId": 1006}]})
caso("Busqueda paginada de matriculas", "GET", "/api/v1/matriculas?periodoId=4&sortBy=fecha&direction=desc&size=5", 200,
     verificar=lambda b, h: f"total={b['page']['totalElements'] if 'page' in b else b.get('totalElements')}")
caso("Reporte agregado de matriculas", "GET", "/api/v1/matriculas/reporte?periodoId=4", 200,
     verificar=lambda b, h: f"matriculas={b['totalMatriculas']} detalles={b['totalDetalles']}")
caso("Ocupacion de secciones (reporte)", "GET", "/api/v1/secciones/resumen?periodoId=4", 200)

cargo_ana = m1["cargo"]["cargoId"]
caso("Cargo de Ana con cuotas", "GET", f"/api/v1/finanzas/cargos/{cargo_ana}", 200,
     verificar=lambda b, h: "cuotas=" + ",".join(str(c["montoCuota"]) for c in b["cuotas"]))
caso("Pago parcial", "POST", f"/api/v1/finanzas/cargos/{cargo_ana}/pagos", 201,
     {"monto": 200.00, "medioPago": "EFECTIVO", "referencia": "BOL-0001"},
     verificar=lambda b, h: f"estado={b['estado']} saldo={b['saldoPendiente']}")
caso("Pago mayor al saldo (409)", "POST", f"/api/v1/finanzas/cargos/{cargo_ana}/pagos", 409,
     {"monto": 99999.00, "medioPago": "EFECTIVO"})
caso("Saldo del estudiante", "GET", "/api/v1/finanzas/cargos/estudiante/1001/saldo", 200,
     verificar=lambda b, h: f"deuda={b['deudaTotal']} pagado={b['totalPagado']} saldo={b['saldoPendiente']}")
caso("Anular matricula con pagos (409)", "PATCH", f"/api/v1/matriculas/{m1['id']}/anular", 409)
caso("Cargo idempotente (no duplica)", "POST", f"/api/v1/matriculas/{m1['id']}/cargo", 200,
     verificar=lambda b, h: f"mismo cargo={b['cargo']['cargoId'] == cargo_ana}")
caso("Cargo de matricula previa sin creditos (409)", "POST", "/api/v1/matriculas/1/cargo", 409)
caso("Resumen financiero por estado", "GET", "/api/v1/finanzas/cargos/resumen?periodoId=4", 200)

# --- CORS (encabezados reales del servidor) ---
caso("CORS origen permitido 4200", "OPTIONS", "/api/v1/cursos", 200,
     headers={"Origin": "http://localhost:4200", "Access-Control-Request-Method": "GET"},
     verificar=lambda b, h: "Access-Control-Allow-Origin=" + str(h.get("Access-Control-Allow-Origin")))
caso("CORS origen no permitido 5500 (403)", "OPTIONS", "/api/v1/cursos", 403,
     headers={"Origin": "http://localhost:5500", "Access-Control-Request-Method": "GET"})

print("| # | Caso | Peticion | Esperado | Obtenido | Resultado | Evidencia |")
print("|---|---|---|---|---|---|---|")
for i, f in enumerate(filas, 1):
    print(f"| {i} | {f[0]} | `{f[1]}` | {f[2]} | {f[3]} | {f[4]} | {str(f[5]).replace('|', '/')} |")
print(f"\nTotal: {len(filas)} casos, {len(filas) - fallos} OK, {fallos} fallas")
sys.exit(1 if fallos else 0)
