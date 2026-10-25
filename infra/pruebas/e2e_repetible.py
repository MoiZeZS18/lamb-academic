"""
Pruebas HTTP de punta a punta REPETIBLES contra el backend LAMB levantado (http://localhost:8080).

- Crea sus propios datos con codigos unicos por ejecucion (sufijo aleatorio) y aulas/horarios
  exclusivos (SABADO, aulas E2E-*), sin tocar los datos demo ni las matriculas previas.
- Los datos demo y legados solo se LEEN (matricula 1, conceptos de pago, periodo 2026-II).
- Al final elimina SOLO los ids que creo (SQL*Plus en el contenedor) y verifica que el conteo
  de cada tabla vuelva exactamente al valor inicial.

Uso (desde la raiz del repo, con el backend corriendo y Docker activo):
    python -I infra/pruebas/e2e_repetible.py
La clave de LAMB_APP se lee de backend/.env (no se imprime).
"""
import json
import pathlib
import random
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import keycloak_pkce  # noqa: E402  (U2: el backend exige un token de Keycloak)

BASE = "http://localhost:8080"
RAIZ = pathlib.Path(__file__).resolve().parents[2]
SUF = f"{random.randint(0, 99999):05d}"
TABLAS = ["SEDE", "FACULTAD", "ESCUELA", "PERSONA", "ALUMNOS", "EXPEDIENTES", "DOCUMENTOS_ADJUNTOS", "CURSO",
          "PLAN_ESTUDIO", "DETALLE_PLAN_ESTUDIO", "PERIODOS", "SECCIONES", "HORARIOS_AULA", "MATRICULAS",
          "DETALLE_MATRICULA", "FIN_CONCEPTOS_PAGO", "FIN_CARGOS", "FIN_DETALLE_CARGOS", "FIN_PAGOS"]

filas = []
fallos = 0
creados = {k: [] for k in ["persona", "alumno", "curso", "seccion", "plan", "periodo", "expediente"]}
inst = {}
contador = [0]


def n():
    contador[0] += 1
    return contador[0]


# ------------------------------- Oracle (SQL*Plus) -------------------------------

def clave_bd():
    for linea in (RAIZ / "backend" / ".env").read_text(encoding="utf-8").splitlines():
        if linea.startswith("LAMB_DB_PASSWORD="):
            return linea.split("=", 1)[1].strip()
    raise SystemExit("No se encontro LAMB_DB_PASSWORD en backend/.env")


def sqlplus(sql):
    r = subprocess.run(["docker", "exec", "-i", "lamb-academic-oracle", "sqlplus", "-s",
                        f"LAMB_APP/{clave_bd()}@localhost:1521/FREEPDB1"],
                       input="set heading off feedback off pagesize 0\nWHENEVER SQLERROR EXIT FAILURE ROLLBACK\n"
                             + sql + "\nEXIT\n", capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError("SQL*Plus fallo: " + r.stdout[-500:] + r.stderr[-500:])
    return r.stdout


def conteos():
    sql = "\n".join(f"SELECT '{t}=' || COUNT(*) FROM {t};" for t in TABLAS)
    salida = {}
    for linea in sqlplus(sql).split():
        if "=" in linea:
            k, v = linea.split("=")
            salida[k] = int(v)
    return salida


def lista(ids):
    return ",".join(str(i) for i in ids) or "-1"


def limpiar():
    alumnos = lista(creados["alumno"])
    sql = f"""
DELETE FROM FIN_PAGOS WHERE ID_CARGO IN (SELECT ID FROM FIN_CARGOS WHERE ID_ESTUDIANTE IN ({alumnos}));
DELETE FROM FIN_DETALLE_CARGOS WHERE ID_CARGO IN (SELECT ID FROM FIN_CARGOS WHERE ID_ESTUDIANTE IN ({alumnos}));
DELETE FROM FIN_CARGOS WHERE ID_ESTUDIANTE IN ({alumnos});
DELETE FROM DETALLE_MATRICULA WHERE MATRICULA_ID IN (SELECT ID FROM MATRICULAS WHERE ESTUDIANTE_ID IN ({alumnos}));
DELETE FROM MATRICULAS WHERE ESTUDIANTE_ID IN ({alumnos});
DELETE FROM DETALLE_PLAN_ESTUDIO WHERE ID_PLAN IN ({lista(creados['plan'])});
DELETE FROM PLAN_ESTUDIO WHERE ID IN ({lista(creados['plan'])});
DELETE FROM HORARIOS_AULA WHERE ID_SECCION IN ({lista(creados['seccion'])});
DELETE FROM SECCIONES WHERE ID IN ({lista(creados['seccion'])});
DELETE FROM PERIODOS WHERE ID IN ({lista(creados['periodo'])});
DELETE FROM DOCUMENTOS_ADJUNTOS WHERE ID_EXPEDIENTE IN ({lista(creados['expediente'])});
DELETE FROM EXPEDIENTES WHERE ID IN ({lista(creados['expediente'])});
DELETE FROM ALUMNOS WHERE ID IN ({alumnos});
DELETE FROM PERSONA WHERE ID IN ({lista(creados['persona'])});
DELETE FROM CURSO WHERE ID IN ({lista(creados['curso'])});
DELETE FROM ESCUELA WHERE ID IN ({inst.get('escuela', -1)});
DELETE FROM FACULTAD WHERE ID IN ({inst.get('facultad', -1)});
DELETE FROM SEDE WHERE ID IN ({inst.get('sede', -1)});
COMMIT;
"""
    sqlplus(sql)


# ------------------------------------ HTTP ------------------------------------

# U2: las pruebas operan como ADMIN (el unico rol con acceso a todos los modulos). El token se obtiene
# con Authorization Code + PKCE (keycloak_pkce.py) y se renueva con el refresh_token antes de vencer.
_sesion = {"tokens": None, "vence": 0.0}
_candado_token = threading.Lock()


def token_admin():
    with _candado_token:
        if _sesion["tokens"] is None:
            _sesion["tokens"] = keycloak_pkce.login("admin.lamb")
        elif time.time() > _sesion["vence"] - 30:
            estado, nuevos = keycloak_pkce.refrescar(_sesion["tokens"]["refresh_token"])
            _sesion["tokens"] = nuevos if estado == 200 else keycloak_pkce.login("admin.lamb")
        else:
            return _sesion["tokens"]["access_token"]
        _sesion["vence"] = time.time() + _sesion["tokens"]["expires_in"]
        return _sesion["tokens"]["access_token"]


def llamar(metodo, ruta, cuerpo=None, headers=None):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", f"Bearer {token_admin()}")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            texto, estado, hdrs = r.read().decode("utf-8"), r.status, dict(r.headers)
    except urllib.error.HTTPError as e:
        texto, estado, hdrs = e.read().decode("utf-8"), e.code, dict(e.headers)
    try:
        cuerpo_resp = json.loads(texto) if texto else None
    except ValueError:
        cuerpo_resp = texto
    return estado, cuerpo_resp, hdrs


def caso(grupo, nombre, metodo, ruta, esperado, cuerpo=None, headers=None, verificar=None):
    global fallos
    estado, body, hdrs = llamar(metodo, ruta, cuerpo, headers)
    ok, detalle = estado == esperado, ""
    if ok and verificar:
        try:
            detalle = verificar(body, hdrs) or ""
        except (AssertionError, KeyError, TypeError, IndexError) as ex:
            ok, detalle = False, f"verificacion fallida: {ex!r}"
    if not ok and not detalle:
        detalle = (body.get("message") if isinstance(body, dict) else str(body))[:140]
    if not ok:
        fallos += 1
    filas.append((grupo, nombre, f"{metodo} {ruta}", esperado, estado, "OK" if ok else "FALLA", detalle))
    return body


def afirmar(condicion, mensaje):
    if not condicion:
        raise AssertionError(mensaje)
    return mensaje


def cupos(seccion_id):
    return llamar("GET", f"/api/v1/secciones/{seccion_id}")[1]["cuposOcupados"]


def persona(tipo, nombre):
    dni = "6" + SUF + f"{n():02d}"
    b = caso("Base Universal", f"Crear persona {tipo} {nombre}", "POST", "/api/v1/personas", 201, {
        "tipoPersona": tipo, "tipoDocumento": "DNI", "numeroDocumento": dni, "nombres": nombre,
        "apellidoPaterno": "E2E", "idEscuela": inst["escuela"]})
    creados["persona"].append(b["id"])
    return b


def alumno(nombre):
    p = persona("ALUMNO", nombre)
    b = caso("Base Universal", f"Registrar alumno {nombre}", "POST", "/api/v1/alumnos", 201, {
        "personaId": p["id"], "codigoEstudiante": f"E2E{SUF}{n()}", "fechaIngreso": "2025-03-01"})
    creados["alumno"].append(b["id"])
    return b["id"], p["id"]


def curso(creditos):
    b = caso("Core Academico", f"Crear curso de {creditos} creditos", "POST", "/api/v1/cursos", 201, {
        "codigo": f"E{SUF}{n()}", "nombre": f"Curso E2E {contador[0]}", "creditos": creditos,
        "horasTeoricas": 2, "horasPracticas": 2})
    creados["curso"].append(b["id"])
    return b["id"]


def seccion(curso_id, periodo_id, vacantes, inicio, fin, docente=None, esperado=201, nombre=None, aula=None):
    b = caso("Core Academico", nombre or f"Programar seccion ({vacantes} vacantes)", "POST", "/api/v1/secciones",
             esperado, {"cursoId": curso_id, "periodoId": periodo_id, "grupo": f"E{n()}", "docenteId": docente,
                        "limiteVacantes": vacantes,
                        "horarios": [{"diaSemana": "SABADO", "horaInicio": inicio, "horaFin": fin,
                                      "aula": aula or f"E2E-{SUF}-{contador[0]}"}]})
    if esperado == 201:
        creados["seccion"].append(b["id"])
        return b["id"]
    return None


def matricula(alumno_id, periodo_id, secciones, esperado, nombre, fecha="2026-10-24", cuotas=None, verificar=None):
    m = {"estudianteId": alumno_id, "periodoId": periodo_id, "fecha": fecha, "estado": "INICIADA"}
    if cuotas:
        m["numeroCuotas"] = cuotas
    return caso("Matricula", nombre, "POST", "/api/v1/matriculas", esperado,
                {"matricula": m, "detalles": [{"seccionId": s} for s in secciones]}, verificar=verificar)


# ----------------------------------- Escenario -----------------------------------

def escenario():
    periodo = llamar("GET", "/api/v1/periodos")[1]
    activo = next(p["id"] for p in periodo if p["estado"] == "ACTIVO")

    # Verificacion y documentacion
    caso("Plataforma", "Salud con Oracle", "GET", "/actuator/health", 200,
         verificar=lambda b, h: afirmar(b["components"]["db"]["status"] == "UP", "status=UP db=UP"))
    caso("Plataforma", "OpenAPI publicado", "GET", "/v3/api-docs", 200,
         verificar=lambda b, h: f"{len(b['paths'])} rutas")
    caso("Plataforma", "Endpoint de verificacion", "GET", "/api/v1/hello", 200)
    caso("Plataforma", "Modulos Spring Modulith", "GET", "/actuator/modulith", 200,
         verificar=lambda b, h: afirmar({"baseuniversal", "coreacademico", "matricula", "finanzas"} <= set(b),
                                        "4 modulos de negocio"))

    # Base Universal: institucional (relaciones)
    inst["sede"] = caso("Base Universal", "Crear sede", "POST", "/api/v1/sedes", 201,
                        {"codigo": f"E2ES{SUF}", "nombre": "Sede E2E"})["id"]
    inst["facultad"] = caso("Base Universal", "Crear facultad (relacion con sede)", "POST", "/api/v1/facultades", 201,
                            {"idSede": inst["sede"], "codigo": f"E2EF{SUF}", "nombre": "Facultad E2E"})["id"]
    inst["escuela"] = caso("Base Universal", "Crear escuela (relacion con facultad)", "POST", "/api/v1/escuelas", 201,
                           {"idFacultad": inst["facultad"], "codigo": f"E2EE{SUF}", "nombre": "Escuela E2E"})["id"]
    caso("Base Universal", "Desactivar sede con facultades activas (409)", "DELETE", f"/api/v1/sedes/{inst['sede']}", 409)
    caso("Base Universal", "Arbol institucional", "GET", "/api/v1/institucional/arbol", 200,
         verificar=lambda b, h: afirmar(any(s["id"] == inst["sede"] for s in b), "incluye la sede creada"))

    # Base Universal: personas y alumnos
    ana_id, ana_persona = alumno("Ana")
    beto_id, _ = alumno("Beto")
    caro_id, _ = alumno("Caro")
    dani_id, _ = alumno("Dani")
    eva_id, _ = alumno("Eva")
    docente = persona("DOCENTE", "Doc")["id"]
    caso("Base Universal", "Persona con DNI invalido (400)", "POST", "/api/v1/personas", 400, {
        "tipoPersona": "ALUMNO", "tipoDocumento": "DNI", "numeroDocumento": "12AB", "nombres": "X",
        "apellidoPaterno": "Y"}, verificar=lambda b, h: "errores=" + ",".join(b["errores"]))
    dni_ana = llamar("GET", f"/api/v1/personas/{ana_persona}")[1]["numeroDocumento"]
    caso("Base Universal", "Documento duplicado (409)", "POST", "/api/v1/personas", 409, {
        "tipoPersona": "ALUMNO", "tipoDocumento": "DNI", "numeroDocumento": dni_ana, "nombres": "Copia",
        "apellidoPaterno": "E2E"})
    caso("Base Universal", "Persona inexistente (404)", "GET", "/api/v1/personas/999999999", 404)
    caso("Base Universal", "Cambiar a DOCENTE una persona que ya es alumno (409)", "PUT",
         f"/api/v1/personas/{ana_persona}", 409, {"tipoPersona": "DOCENTE", "tipoDocumento": "DNI",
                                                  "numeroDocumento": dni_ana, "nombres": "Ana", "apellidoPaterno": "E2E"})
    caso("Base Universal", "Docente como alumno (409)", "POST", "/api/v1/alumnos", 409, {
        "personaId": docente, "codigoEstudiante": f"E2EX{SUF}", "fechaIngreso": "2025-03-01"})
    caso("Base Universal", "Filtro combinado de personas + orden", "GET",
         f"/api/v1/personas?tipoPersona=ALUMNO&idEscuela={inst['escuela']}&q=e2e&sortBy=nombres&dir=asc", 200,
         verificar=lambda b, h: afirmar([p["nombres"] for p in b] == ["Ana", "Beto", "Caro", "Dani", "Eva"],
                                        "5 alumnos ordenados por nombre"))

    # Base Universal: expediente cabecera-detalle + rollback
    exp = caso("Base Universal", "Expediente con 2 documentos (cabecera-detalle)", "POST", "/api/v1/expedientes", 201, {
        "codigoTramite": f"E2E-EXP-{SUF}", "asunto": "Constancia", "personaId": ana_persona, "documentos": [
            {"nombre": "DNI", "nombreArchivo": "dni.pdf", "tipoDocumento": "PDF", "rutaArchivo": "/e2e/dni.pdf"},
            {"nombre": "Foto", "nombreArchivo": "foto.jpg", "tipoDocumento": "JPG", "rutaArchivo": "/e2e/foto.jpg"}]},
        verificar=lambda b, h: afirmar(b["totalDocumentos"] == 2, "totalDocumentos=2"))
    creados["expediente"].append(exp["id"])
    caso("Base Universal", "Expediente con archivo inconsistente: rollback (409)", "POST", "/api/v1/expedientes", 409, {
        "codigoTramite": f"E2E-EXR-{SUF}", "asunto": "Rollback", "personaId": ana_persona, "documentos": [
            {"nombre": "Ok", "nombreArchivo": "ok.pdf", "tipoDocumento": "PDF", "rutaArchivo": "/e2e/ok.pdf"},
            {"nombre": "Mal", "nombreArchivo": "mal.png", "tipoDocumento": "PDF", "rutaArchivo": "/e2e/mal.png"}]})
    caso("Base Universal", "Rollback verificado: la cabecera no quedo", "GET", f"/api/v1/expedientes?personaId={ana_persona}",
         200, verificar=lambda b, h: afirmar([x["codigoTramite"] for x in b] == [f"E2E-EXP-{SUF}"], "solo EXP"))
    caso("Base Universal", "Flujo de estado del expediente", "PATCH", f"/api/v1/expedientes/{exp['id']}/estado", 200,
         {"estado": "EN_REVISION"})

    # Core Academico: cursos, malla con prerrequisito
    c_prev, c_adv, c_r, c_s, c_u, c_v = curso(4), curso(4), curso(3), curso(3), curso(2), curso(2)
    caso("Core Academico", "Malla con curso repetido: rollback (409)", "POST", "/api/v1/planes-estudio", 409, {
        "idEscuela": inst["escuela"], "codigo": f"E2E-PX-{SUF}", "nombre": "Malla invalida", "anio": 2026,
        "detalles": [{"idCurso": c_prev, "ciclo": 1, "creditos": 4}, {"idCurso": c_prev, "ciclo": 1, "creditos": 4}]})
    plan = caso("Core Academico", "Malla con prerrequisito (cabecera-detalle)", "POST", "/api/v1/planes-estudio", 201, {
        "idEscuela": inst["escuela"], "codigo": f"E2E-PL-{SUF}", "nombre": "Malla E2E", "anio": 2026, "detalles": [
            {"idCurso": c_prev, "ciclo": 1, "creditos": 4}, {"idCurso": c_r, "ciclo": 1, "creditos": 3},
            {"idCurso": c_adv, "ciclo": 2, "creditos": 4, "idPrerrequisito": c_prev},
            {"idCurso": c_s, "ciclo": 2, "creditos": 3}]},
        verificar=lambda b, h: afirmar(b["totalCreditos"] == 14 and b["totalCursos"] == 4, "14 creditos, 4 cursos"))
    creados["plan"].append(plan["id"])
    caso("Core Academico", "Prerrequisitos del curso avanzado", "GET",
         f"/api/v1/cursos/{c_adv}/prerrequisitos?idEscuela={inst['escuela']}", 200,
         verificar=lambda b, h: afirmar([c["id"] for c in b] == [c_prev], "prerrequisito = curso previo"))
    caso("Core Academico", "Distribucion de la malla por ciclo", "GET", f"/api/v1/planes-estudio/{plan['id']}/distribucion",
         200, verificar=lambda b, h: afirmar([d["creditos"] for d in b] == [7, 7], "7 + 7 creditos"))

    # Periodo pasado propio (se cierra despues para registrar resultados)
    previo = caso("Matricula", "Crear periodo pasado propio", "POST", "/api/v1/periodos", 201, {
        "codigo": f"E2E{SUF}", "nombre": "Periodo E2E", "fechaInicio": "2025-03-01", "fechaFin": "2025-07-15",
        "estado": "ACTIVO"})
    creados["periodo"].append(previo["id"])
    caso("Matricula", "Periodo con fechas invertidas (409)", "POST", "/api/v1/periodos", 409, {
        "codigo": f"E2EX{SUF}", "nombre": "Mal", "fechaInicio": "2025-07-15", "fechaFin": "2025-03-01",
        "estado": "ACTIVO"})

    # Secciones (horarios del sabado en aulas exclusivas)
    s_prev = seccion(c_prev, previo["id"], 10, "08:00", "10:00")
    s_adv = seccion(c_adv, activo, 5, "08:00", "10:00", docente=docente)
    s_r = seccion(c_r, activo, 1, "10:00", "12:00")
    s_s = seccion(c_s, activo, 5, "13:00", "15:00")
    s_u = seccion(c_u, activo, 5, "14:00", "16:00")
    s_v = seccion(c_v, activo, 1, "16:00", "17:00")
    aula_adv = llamar("GET", f"/api/v1/secciones/{s_adv}")[1]["horarios"][0]["aula"]
    seccion(c_v, activo, 5, "09:00", "11:00", esperado=409, nombre="Seccion con aula ocupada (409)", aula=aula_adv)
    seccion(c_v, activo, 5, "09:00", "10:00", docente=docente, esperado=409, nombre="Seccion con docente ocupado (409)")
    caso("Core Academico", "Carga docente", "GET", f"/api/v1/docentes/{docente}/carga?periodoId={activo}", 200,
         verificar=lambda b, h: afirmar(b["totalSecciones"] == 1 and b["horasSemanales"] == 2.0, "1 seccion, 2 h"))

    # Historial: matricula en el periodo pasado, resultados al cerrarlo
    m_ana_prev = matricula(ana_id, previo["id"], [s_prev], 201, "Ana en el periodo pasado", fecha="2025-04-01", cuotas=1)
    m_beto_prev = matricula(beto_id, previo["id"], [s_prev], 201, "Beto en el periodo pasado", fecha="2025-04-01", cuotas=1)
    det_ana = m_ana_prev["detalles"][0]["id"]
    det_beto = m_beto_prev["detalles"][0]["id"]
    caso("Matricula", "Resultado con periodo abierto (409)", "PATCH",
         f"/api/v1/matriculas/{m_ana_prev['id']}/detalles/{det_ana}/resultado", 409, {"resultado": "APROBADO"})
    caso("Matricula", "Cerrar el periodo pasado", "PUT", f"/api/v1/periodos/{previo['id']}", 200, {
        "codigo": previo["codigo"], "nombre": previo["nombre"], "fechaInicio": previo["fechaInicio"],
        "fechaFin": previo["fechaFin"], "estado": "CERRADO"})
    caso("Matricula", "Resultado APROBADO (Ana)", "PATCH",
         f"/api/v1/matriculas/{m_ana_prev['id']}/detalles/{det_ana}/resultado", 200, {"resultado": "APROBADO"},
         verificar=lambda b, h: afirmar(b["detalles"][0]["resultado"] == "APROBADO", "resultado=APROBADO"))
    caso("Matricula", "Resultado DESAPROBADO (Beto)", "PATCH",
         f"/api/v1/matriculas/{m_beto_prev['id']}/detalles/{det_beto}/resultado", 200, {"resultado": "DESAPROBADO"})
    caso("Matricula", "Resultado invalido (400)", "PATCH",
         f"/api/v1/matriculas/{m_beto_prev['id']}/detalles/{det_beto}/resultado", 400, {"resultado": "EXCELENTE"})

    # Base -> Core -> Matricula -> Finanzas en el periodo activo
    antes_adv = cupos(s_adv)
    m_ana = matricula(ana_id, activo, [s_adv, s_r], 201, "Ana: avanzado (prerrequisito aprobado) + R, con cargo",
                      cuotas=3, verificar=lambda b, h: afirmar(b["totalCreditos"] == 7 and b["cargo"]["numeroCuotas"] == 3,
                                                               f"7 creditos, cargo {b['cargo']['montoTotal']} en 3 cuotas"))
    caso("Matricula", "Reserva de cupos aplicada", "GET", f"/api/v1/secciones/{s_r}", 200,
         verificar=lambda b, h: afirmar(b["cuposOcupados"] == 1 and cupos(s_adv) == antes_adv + 1, "R=1/1, avanzado +1"))
    caso("Matricula", "GET de la matricula incluye su cargo (regresion cargo null)", "GET",
         f"/api/v1/matriculas/{m_ana['id']}", 200,
         verificar=lambda b, h: afirmar(b["cargo"] and b["cargo"]["cargoId"] == m_ana["cargo"]["cargoId"],
                                        f"cargo {b['cargo']['cargoId'] if b['cargo'] else None}"))
    caso("Matricula", "Listado paginado incluye el cargo de cada matricula", "GET",
         f"/api/v1/matriculas?estudianteId={ana_id}&periodoId={activo}", 200,
         verificar=lambda b, h: afirmar([m["cargo"]["cargoId"] for m in b["content"]] == [m_ana["cargo"]["cargoId"]],
                                        "cargo presente en el listado"))
    matricula(ana_id, activo, [s_s], 409, "Matricula duplicada en el periodo (409)")
    matricula(beto_id, activo, [s_adv], 409, "Prerrequisito DESAPROBADO (409)")
    matricula(caro_id, activo, [s_adv], 409, "Prerrequisito nunca llevado (409)")
    matricula(caro_id, activo, [s_s, s_u], 409, "Cruce de horario S/U (409)")
    antes_s = cupos(s_s)
    matricula(caro_id, activo, [s_s, s_r], 409, "Seccion R llena: se rechaza toda la solicitud (409)")
    caso("Matricula", "Sin reservas parciales: S no quedo reservada", "GET", f"/api/v1/secciones/{s_s}", 200,
         verificar=lambda b, h: afirmar(b["cuposOcupados"] == antes_s, f"cupos S={b['cuposOcupados']}"))
    m_caro = matricula(caro_id, activo, [s_s], 201, "Caro en S")
    matricula(caro_id, activo, [], 400, "Matricula sin detalles (400)")
    matricula(999999, activo, [s_s], 404, "Estudiante inexistente (404)")
    matricula(dani_id, activo, [s_s], 409, "Fecha fuera del periodo (409)", fecha="2027-01-10")

    # Concurrencia por HTTP: dos estudiantes por el ultimo cupo de V
    resultados = []
    barrera = threading.Barrier(2)

    def competir(alumno_id):
        barrera.wait()
        resultados.append(llamar("POST", "/api/v1/matriculas", {
            "matricula": {"estudianteId": alumno_id, "periodoId": activo, "fecha": "2026-10-24", "estado": "INICIADA"},
            "detalles": [{"seccionId": s_v}]})[0])

    hilos = [threading.Thread(target=competir, args=(a,)) for a in (dani_id, eva_id)]
    for t in hilos:
        t.start()
    for t in hilos:
        t.join()
    global fallos
    ok = sorted(resultados) == [201, 409] and cupos(s_v) == 1
    fallos += 0 if ok else 1
    filas.append(("Matricula", "Concurrencia: 2 solicitudes simultaneas por el ultimo cupo", "POST /api/v1/matriculas x2",
                  "201+409", "+".join(map(str, sorted(resultados))), "OK" if ok else "FALLA", f"cupos V={cupos(s_v)}/1"))

    # Finanzas
    cargo_ana = m_ana["cargo"]["cargoId"]
    caso("Finanzas", "Cuotas del cargo suman el total", "GET", f"/api/v1/finanzas/cargos/{cargo_ana}", 200,
         verificar=lambda b, h: afirmar(round(sum(c["montoCuota"] for c in b["cuotas"]), 2) == b["montoTotal"],
                                        "cuotas=" + ",".join(str(c["montoCuota"]) for c in b["cuotas"])))
    caso("Finanzas", "Pago parcial", "POST", f"/api/v1/finanzas/cargos/{cargo_ana}/pagos", 201,
         {"monto": 100.00, "medioPago": "EFECTIVO", "referencia": f"E2E-{SUF}"},
         verificar=lambda b, h: afirmar(b["estado"] == "PAGADO_PARCIAL", f"saldo={b['saldoPendiente']}"))
    caso("Finanzas", "Pago mayor al saldo (409)", "POST", f"/api/v1/finanzas/cargos/{cargo_ana}/pagos", 409,
         {"monto": 99999.00, "medioPago": "EFECTIVO"})
    caso("Finanzas", "Saldo del estudiante", "GET", f"/api/v1/finanzas/cargos/estudiante/{ana_id}/saldo", 200,
         verificar=lambda b, h: afirmar(b["totalPagado"] == 100, f"deuda={b['deudaTotal']} saldo={b['saldoPendiente']}"))
    caso("Finanzas", "Saldo de estudiante inexistente (404)", "GET", "/api/v1/finanzas/cargos/estudiante/999999/saldo", 404)
    caso("Finanzas", "Cargo manual de MATRICULA (409, evita duplicar deuda)", "POST", "/api/v1/finanzas/cargos", 409, {
        "estudianteId": ana_id, "procesoOrigen": "MATRICULA", "periodoId": activo, "totalCreditos": 7, "numeroCuotas": 1})
    caso("Finanzas", "Cargo manual de TRAMITE con concepto", "POST", "/api/v1/finanzas/cargos", 201, {
        "estudianteId": ana_id, "procesoOrigen": "TRAMITE", "periodoId": activo, "numeroCuotas": 1,
        "conceptos": ["CARNE"]}, verificar=lambda b, h: f"monto={b['montoTotal']}")
    caso("Finanzas", "Cargo idempotente (no duplica)", "POST", f"/api/v1/matriculas/{m_ana['id']}/cargo", 200,
         verificar=lambda b, h: afirmar(b["cargo"]["cargoId"] == cargo_ana, "mismo cargo"))
    caso("Finanzas", "Anular matricula con pagos (409)", "PATCH", f"/api/v1/matriculas/{m_ana['id']}/anular", 409)
    antes_s = cupos(s_s)
    caso("Matricula", "Anular matricula sin pagos: libera cupo y anula cargo", "PATCH",
         f"/api/v1/matriculas/{m_caro['id']}/anular", 200,
         verificar=lambda b, h: afirmar(b["estado"] == "ANULADA" and cupos(s_s) == antes_s - 1, "cupo liberado"))
    caso("Finanzas", "Cargo de la matricula anulada queda ANULADO", "GET", f"/api/v1/finanzas/cargos/estudiante/{caro_id}",
         200, verificar=lambda b, h: afirmar([c["estado"] for c in b] == ["ANULADO"], "ANULADO"))
    caso("Matricula", "numeroCuotas fuera de rango (400)", "POST", f"/api/v1/matriculas/{m_ana['id']}/cargo?numeroCuotas=0", 400)

    # Datos previos: solo lectura
    caso("Compatibilidad", "Matricula previa 1 se consulta", "GET", "/api/v1/matriculas/1", 200,
         verificar=lambda b, h: afirmar(b["estado"] == "INICIADA" and b["detalles"][0]["cursoId"] is None,
                                        "INICIADA, detalles sin curso"))
    caso("Compatibilidad", "Matricula previa 1: cargo no calculable (409)", "POST", "/api/v1/matriculas/1/cargo", 409)

    # Consultas y reportes
    caso("Consultas", "Busqueda paginada de matriculas (contrato VIA_DTO)", "GET",
         f"/api/v1/matriculas?periodoId={activo}&sortBy=fecha&direction=desc&size=5", 200,
         verificar=lambda b, h: afirmar("page" in b and "content" in b and len(b["content"]) <= 5,
                                        f"page.totalElements={b['page']['totalElements']}"))
    caso("Consultas", "Reporte agregado de matriculas", "GET", f"/api/v1/matriculas/reporte?periodoId={activo}", 200)
    caso("Consultas", "Resumen de matriculas", "GET", "/api/v1/matriculas/resumen", 200)
    caso("Consultas", "Secciones con vacantes (filtros combinados)", "GET",
         f"/api/v1/secciones?periodoId={activo}&docenteId={docente}&conVacantes=true", 200,
         verificar=lambda b, h: afirmar([s["id"] for s in b] == [s_adv], "solo la seccion del docente"))
    caso("Consultas", "Ocupacion por curso (reporte)", "GET", f"/api/v1/secciones/resumen?periodoId={activo}", 200)
    caso("Consultas", "Resumen financiero por estado", "GET", f"/api/v1/finanzas/cargos/resumen?periodoId={activo}", 200)
    caso("Consultas", "Cargos filtrados por estudiante y estado", "GET",
         f"/api/v1/finanzas/cargos?estudianteId={ana_id}&estado=PAGADO_PARCIAL", 200,
         verificar=lambda b, h: afirmar(len(b) == 1, "1 cargo"))
    caso("Consultas", "Resumen de personas por tipo", "GET", "/api/v1/personas/resumen", 200)
    caso("Consultas", "Resumen de alumnos por estado", "GET", "/api/v1/alumnos/resumen", 200)
    caso("Consultas", "Resumen de mallas", "GET", "/api/v1/planes-estudio/resumen", 200)
    caso("Consultas", "Resumen de expedientes", "GET", "/api/v1/expedientes/resumen", 200)

    # CORS (encabezados reales)
    caso("CORS", "Origen permitido 4200", "OPTIONS", "/api/v1/cursos", 200,
         headers={"Origin": "http://localhost:4200", "Access-Control-Request-Method": "GET"},
         verificar=lambda b, h: afirmar(h.get("Access-Control-Allow-Origin") == "http://localhost:4200", "ACAO=4200"))
    caso("CORS", "Origen no permitido 5500 (403)", "OPTIONS", "/api/v1/cursos", 403,
         headers={"Origin": "http://localhost:5500", "Access-Control-Request-Method": "GET"})
    caso("Plataforma", "X-Trace-ID propagado", "GET", "/api/v1/hello", 200, headers={"X-Trace-ID": f"e2e-{SUF}"},
         verificar=lambda b, h: afirmar(h.get("X-Trace-ID") == f"e2e-{SUF}", "X-Trace-ID devuelto"))


def main():
    inicial = conteos()
    error = None
    try:
        escenario()
    except Exception as ex:  # el informe y la limpieza se ejecutan igual
        error = ex
    finally:
        limpiar()
        final = conteos()
    diferencias = {t: (inicial[t], final[t]) for t in TABLAS if inicial[t] != final[t]}
    print(f"Ejecucion E2E sufijo {SUF}\n")
    print("| # | Grupo | Caso | Peticion | Esperado | Obtenido | Resultado | Evidencia |")
    print("|---|---|---|---|---|---|---|---|")
    for i, f in enumerate(filas, 1):
        print(f"| {i} | {f[0]} | {f[1]} | `{f[2]}` | {f[3]} | {f[4]} | {f[5]} | {str(f[6]).replace('|', '/')} |")
    print(f"\nTotal: {len(filas)} casos, {len(filas) - fallos} OK, {fallos} fallas")
    print("Limpieza: " + ("conteos de las 19 tablas iguales al inicio" if not diferencias
                          else f"DIFERENCIAS {diferencias}"))
    if error:
        print(f"ERROR en el escenario: {error!r}")
    sys.exit(1 if (fallos or diferencias or error) else 0)


if __name__ == "__main__":
    main()
