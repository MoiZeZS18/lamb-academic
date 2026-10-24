Ejecucion E2E sufijo 09573

| # | Grupo | Caso | Peticion | Esperado | Obtenido | Resultado | Evidencia |
|---|---|---|---|---|---|---|---|
| 1 | Plataforma | Salud con Oracle | `GET /actuator/health` | 200 | 200 | OK | status=UP db=UP |
| 2 | Plataforma | OpenAPI publicado | `GET /v3/api-docs` | 200 | 200 | OK | 54 rutas |
| 3 | Plataforma | Endpoint de verificacion | `GET /api/v1/hello` | 200 | 200 | OK |  |
| 4 | Plataforma | Modulos Spring Modulith | `GET /actuator/modulith` | 200 | 200 | OK | 4 modulos de negocio |
| 5 | Base Universal | Crear sede | `POST /api/v1/sedes` | 201 | 201 | OK |  |
| 6 | Base Universal | Crear facultad (relacion con sede) | `POST /api/v1/facultades` | 201 | 201 | OK |  |
| 7 | Base Universal | Crear escuela (relacion con facultad) | `POST /api/v1/escuelas` | 201 | 201 | OK |  |
| 8 | Base Universal | Desactivar sede con facultades activas (409) | `DELETE /api/v1/sedes/43` | 409 | 409 | OK |  |
| 9 | Base Universal | Arbol institucional | `GET /api/v1/institucional/arbol` | 200 | 200 | OK | incluye la sede creada |
| 10 | Base Universal | Crear persona ALUMNO Ana | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 11 | Base Universal | Registrar alumno Ana | `POST /api/v1/alumnos` | 201 | 201 | OK |  |
| 12 | Base Universal | Crear persona ALUMNO Beto | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 13 | Base Universal | Registrar alumno Beto | `POST /api/v1/alumnos` | 201 | 201 | OK |  |
| 14 | Base Universal | Crear persona ALUMNO Caro | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 15 | Base Universal | Registrar alumno Caro | `POST /api/v1/alumnos` | 201 | 201 | OK |  |
| 16 | Base Universal | Crear persona ALUMNO Dani | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 17 | Base Universal | Registrar alumno Dani | `POST /api/v1/alumnos` | 201 | 201 | OK |  |
| 18 | Base Universal | Crear persona ALUMNO Eva | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 19 | Base Universal | Registrar alumno Eva | `POST /api/v1/alumnos` | 201 | 201 | OK |  |
| 20 | Base Universal | Crear persona DOCENTE Doc | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 21 | Base Universal | Persona con DNI invalido (400) | `POST /api/v1/personas` | 400 | 400 | OK | errores=numeroDocumento |
| 22 | Base Universal | Documento duplicado (409) | `POST /api/v1/personas` | 409 | 409 | OK |  |
| 23 | Base Universal | Persona inexistente (404) | `GET /api/v1/personas/999999999` | 404 | 404 | OK |  |
| 24 | Base Universal | Cambiar a DOCENTE una persona que ya es alumno (409) | `PUT /api/v1/personas/71` | 409 | 409 | OK |  |
| 25 | Base Universal | Docente como alumno (409) | `POST /api/v1/alumnos` | 409 | 409 | OK |  |
| 26 | Base Universal | Filtro combinado de personas + orden | `GET /api/v1/personas?tipoPersona=ALUMNO&idEscuela=43&q=e2e&sortBy=nombres&dir=asc` | 200 | 200 | OK | 5 alumnos ordenados por nombre |
| 27 | Base Universal | Expediente con 2 documentos (cabecera-detalle) | `POST /api/v1/expedientes` | 201 | 201 | OK | totalDocumentos=2 |
| 28 | Base Universal | Expediente con archivo inconsistente: rollback (409) | `POST /api/v1/expedientes` | 409 | 409 | OK |  |
| 29 | Base Universal | Rollback verificado: la cabecera no quedo | `GET /api/v1/expedientes?personaId=71` | 200 | 200 | OK | solo EXP |
| 30 | Base Universal | Flujo de estado del expediente | `PATCH /api/v1/expedientes/21/estado` | 200 | 200 | OK |  |
| 31 | Core Academico | Crear curso de 4 creditos | `POST /api/v1/cursos` | 201 | 201 | OK |  |
| 32 | Core Academico | Crear curso de 4 creditos | `POST /api/v1/cursos` | 201 | 201 | OK |  |
| 33 | Core Academico | Crear curso de 3 creditos | `POST /api/v1/cursos` | 201 | 201 | OK |  |
| 34 | Core Academico | Crear curso de 3 creditos | `POST /api/v1/cursos` | 201 | 201 | OK |  |
| 35 | Core Academico | Crear curso de 2 creditos | `POST /api/v1/cursos` | 201 | 201 | OK |  |
| 36 | Core Academico | Crear curso de 2 creditos | `POST /api/v1/cursos` | 201 | 201 | OK |  |
| 37 | Core Academico | Malla con curso repetido: rollback (409) | `POST /api/v1/planes-estudio` | 409 | 409 | OK |  |
| 38 | Core Academico | Malla con prerrequisito (cabecera-detalle) | `POST /api/v1/planes-estudio` | 201 | 201 | OK | 14 creditos, 4 cursos |
| 39 | Core Academico | Prerrequisitos del curso avanzado | `GET /api/v1/cursos/93/prerrequisitos?idEscuela=43` | 200 | 200 | OK | prerrequisito = curso previo |
| 40 | Core Academico | Distribucion de la malla por ciclo | `GET /api/v1/planes-estudio/43/distribucion` | 200 | 200 | OK | 7 + 7 creditos |
| 41 | Matricula | Crear periodo pasado propio | `POST /api/v1/periodos` | 201 | 201 | OK |  |
| 42 | Matricula | Periodo con fechas invertidas (409) | `POST /api/v1/periodos` | 409 | 409 | OK |  |
| 43 | Core Academico | Programar seccion (10 vacantes) | `POST /api/v1/secciones` | 201 | 201 | OK |  |
| 44 | Core Academico | Programar seccion (5 vacantes) | `POST /api/v1/secciones` | 201 | 201 | OK |  |
| 45 | Core Academico | Programar seccion (1 vacantes) | `POST /api/v1/secciones` | 201 | 201 | OK |  |
| 46 | Core Academico | Programar seccion (5 vacantes) | `POST /api/v1/secciones` | 201 | 201 | OK |  |
| 47 | Core Academico | Programar seccion (5 vacantes) | `POST /api/v1/secciones` | 201 | 201 | OK |  |
| 48 | Core Academico | Programar seccion (1 vacantes) | `POST /api/v1/secciones` | 201 | 201 | OK |  |
| 49 | Core Academico | Seccion con aula ocupada (409) | `POST /api/v1/secciones` | 409 | 409 | OK |  |
| 50 | Core Academico | Seccion con docente ocupado (409) | `POST /api/v1/secciones` | 409 | 409 | OK |  |
| 51 | Core Academico | Carga docente | `GET /api/v1/docentes/76/carga?periodoId=4` | 200 | 200 | OK | 1 seccion, 2 h |
| 52 | Matricula | Ana en el periodo pasado | `POST /api/v1/matriculas` | 201 | 201 | OK |  |
| 53 | Matricula | Beto en el periodo pasado | `POST /api/v1/matriculas` | 201 | 201 | OK |  |
| 54 | Matricula | Resultado con periodo abierto (409) | `PATCH /api/v1/matriculas/132/detalles/135/resultado` | 409 | 409 | OK |  |
| 55 | Matricula | Cerrar el periodo pasado | `PUT /api/v1/periodos/46` | 200 | 200 | OK |  |
| 56 | Matricula | Resultado APROBADO (Ana) | `PATCH /api/v1/matriculas/132/detalles/135/resultado` | 200 | 200 | OK | resultado=APROBADO |
| 57 | Matricula | Resultado DESAPROBADO (Beto) | `PATCH /api/v1/matriculas/133/detalles/136/resultado` | 200 | 200 | OK |  |
| 58 | Matricula | Resultado invalido (400) | `PATCH /api/v1/matriculas/133/detalles/136/resultado` | 400 | 400 | OK |  |
| 59 | Matricula | Ana: avanzado (prerrequisito aprobado) + R, con cargo | `POST /api/v1/matriculas` | 201 | 201 | OK | 7 creditos, cargo 500.0 en 3 cuotas |
| 60 | Matricula | Reserva de cupos aplicada | `GET /api/v1/secciones/1094` | 200 | 200 | OK | R=1/1, avanzado +1 |
| 61 | Matricula | Matricula duplicada en el periodo (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 62 | Matricula | Prerrequisito DESAPROBADO (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 63 | Matricula | Prerrequisito nunca llevado (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 64 | Matricula | Cruce de horario S/U (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 65 | Matricula | Seccion R llena: se rechaza toda la solicitud (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 66 | Matricula | Sin reservas parciales: S no quedo reservada | `GET /api/v1/secciones/1095` | 200 | 200 | OK | cupos S=0 |
| 67 | Matricula | Caro en S | `POST /api/v1/matriculas` | 201 | 201 | OK |  |
| 68 | Matricula | Matricula sin detalles (400) | `POST /api/v1/matriculas` | 400 | 400 | OK |  |
| 69 | Matricula | Estudiante inexistente (404) | `POST /api/v1/matriculas` | 404 | 404 | OK |  |
| 70 | Matricula | Fecha fuera del periodo (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 71 | Matricula | Concurrencia: 2 solicitudes simultaneas por el ultimo cupo | `POST /api/v1/matriculas x2` | 201+409 | 201+409 | OK | cupos V=1/1 |
| 72 | Finanzas | Cuotas del cargo suman el total | `GET /api/v1/finanzas/cargos/49` | 200 | 200 | OK | cuotas=166.66,166.66,166.68 |
| 73 | Finanzas | Pago parcial | `POST /api/v1/finanzas/cargos/49/pagos` | 201 | 201 | OK | saldo=400.0 |
| 74 | Finanzas | Pago mayor al saldo (409) | `POST /api/v1/finanzas/cargos/49/pagos` | 409 | 409 | OK |  |
| 75 | Finanzas | Saldo del estudiante | `GET /api/v1/finanzas/cargos/estudiante/1071/saldo` | 200 | 200 | OK | deuda=850 saldo=750 |
| 76 | Finanzas | Saldo de estudiante inexistente (404) | `GET /api/v1/finanzas/cargos/estudiante/999999/saldo` | 404 | 404 | OK |  |
| 77 | Finanzas | Cargo manual de MATRICULA (409, evita duplicar deuda) | `POST /api/v1/finanzas/cargos` | 409 | 409 | OK |  |
| 78 | Finanzas | Cargo manual de TRAMITE con concepto | `POST /api/v1/finanzas/cargos` | 201 | 201 | OK | monto=25.0 |
| 79 | Finanzas | Cargo idempotente (no duplica) | `POST /api/v1/matriculas/134/cargo` | 200 | 200 | OK | mismo cargo |
| 80 | Finanzas | Anular matricula con pagos (409) | `PATCH /api/v1/matriculas/134/anular` | 409 | 409 | OK |  |
| 81 | Matricula | Anular matricula sin pagos: libera cupo y anula cargo | `PATCH /api/v1/matriculas/135/anular` | 200 | 200 | OK | cupo liberado |
| 82 | Finanzas | Cargo de la matricula anulada queda ANULADO | `GET /api/v1/finanzas/cargos/estudiante/1073` | 200 | 200 | OK | ANULADO |
| 83 | Matricula | numeroCuotas fuera de rango (400) | `POST /api/v1/matriculas/134/cargo?numeroCuotas=0` | 400 | 400 | OK |  |
| 84 | Compatibilidad | Matricula previa 1 se consulta | `GET /api/v1/matriculas/1` | 200 | 200 | OK | INICIADA, detalles sin curso |
| 85 | Compatibilidad | Matricula previa 1: cargo no calculable (409) | `POST /api/v1/matriculas/1/cargo` | 409 | 409 | OK |  |
| 86 | Consultas | Busqueda paginada de matriculas (contrato VIA_DTO) | `GET /api/v1/matriculas?periodoId=4&sortBy=fecha&direction=desc&size=5` | 200 | 200 | OK | page.totalElements=12 |
| 87 | Consultas | Reporte agregado de matriculas | `GET /api/v1/matriculas/reporte?periodoId=4` | 200 | 200 | OK |  |
| 88 | Consultas | Resumen de matriculas | `GET /api/v1/matriculas/resumen` | 200 | 200 | OK |  |
| 89 | Consultas | Secciones con vacantes (filtros combinados) | `GET /api/v1/secciones?periodoId=4&docenteId=76&conVacantes=true` | 200 | 200 | OK | solo la seccion del docente |
| 90 | Consultas | Ocupacion por curso (reporte) | `GET /api/v1/secciones/resumen?periodoId=4` | 200 | 200 | OK |  |
| 91 | Consultas | Resumen financiero por estado | `GET /api/v1/finanzas/cargos/resumen?periodoId=4` | 200 | 200 | OK |  |
| 92 | Consultas | Cargos filtrados por estudiante y estado | `GET /api/v1/finanzas/cargos?estudianteId=1071&estado=PAGADO_PARCIAL` | 200 | 200 | OK | 1 cargo |
| 93 | Consultas | Resumen de personas por tipo | `GET /api/v1/personas/resumen` | 200 | 200 | OK |  |
| 94 | Consultas | Resumen de alumnos por estado | `GET /api/v1/alumnos/resumen` | 200 | 200 | OK |  |
| 95 | Consultas | Resumen de mallas | `GET /api/v1/planes-estudio/resumen` | 200 | 200 | OK |  |
| 96 | Consultas | Resumen de expedientes | `GET /api/v1/expedientes/resumen` | 200 | 200 | OK |  |
| 97 | CORS | Origen permitido 4200 | `OPTIONS /api/v1/cursos` | 200 | 200 | OK | ACAO=4200 |
| 98 | CORS | Origen no permitido 5500 (403) | `OPTIONS /api/v1/cursos` | 403 | 403 | OK |  |
| 99 | Plataforma | X-Trace-ID propagado | `GET /api/v1/hello` | 200 | 200 | OK | X-Trace-ID devuelto |

Total: 99 casos, 99 OK, 0 fallas
Limpieza: conteos de las 19 tablas iguales al inicio

---

Segunda ejecucion (repetibilidad):
Ejecucion E2E sufijo 26155
Total: 99 casos, 99 OK, 0 fallas
Limpieza: conteos de las 19 tablas iguales al inicio

Tercera ejecucion (tras la suite final de 83 pruebas):
Ejecucion E2E sufijo 57258
Total: 99 casos, 99 OK, 0 fallas
Limpieza: conteos de las 19 tablas iguales al inicio

---

Ejecucion tras el cierre de seguridad (claves rotadas, correccion del cargo):
Ejecucion E2E sufijo 62979
Total: 101 casos, 101 OK, 0 fallas
Limpieza: conteos de las 19 tablas iguales al inicio
