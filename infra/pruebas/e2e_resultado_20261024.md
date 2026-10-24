| # | Caso | Peticion | Esperado | Obtenido | Resultado | Evidencia |
|---|---|---|---|---|---|---|
| 1 | Salud con Oracle | `GET /actuator/health` | 200 | 200 | OK | status=UP db=UP |
| 2 | OpenAPI publicado | `GET /v3/api-docs` | 200 | 200 | OK | 53 rutas documentadas |
| 3 | Endpoint de verificacion | `GET /api/v1/hello` | 200 | 200 | OK |  |
| 4 | Modulos Modulith (actuator) | `GET /actuator/modulith` | 200 | 200 | OK | modulos=baseuniversal,compartido,coreacademico,exception,filter,finanzas,matricula |
| 5 | Arbol institucional | `GET /api/v1/institucional/arbol` | 200 | 200 | OK | sedes=1 |
| 6 | Crear persona | `POST /api/v1/personas` | 201 | 201 | OK |  |
| 7 | Persona con DNI invalido (400) | `POST /api/v1/personas` | 400 | 400 | OK | errores=numeroDocumento |
| 8 | Persona con documento duplicado (409) | `POST /api/v1/personas` | 409 | 409 | OK |  |
| 9 | Persona inexistente (404) | `GET /api/v1/personas/999999` | 404 | 404 | OK |  |
| 10 | Filtro combinado de personas | `GET /api/v1/personas?tipoPersona=ALUMNO&q=apaza&sortBy=nombres&dir=asc` | 200 | 200 | OK | resultados=1 |
| 11 | Registrar alumno | `POST /api/v1/alumnos` | 201 | 201 | OK |  |
| 12 | Docente como alumno (409) | `POST /api/v1/alumnos` | 409 | 409 | OK |  |
| 13 | Expediente cabecera-detalle | `POST /api/v1/expedientes` | 201 | 201 | OK | totalDocumentos=2 |
| 14 | Expediente con archivo inconsistente: rollback (409) | `POST /api/v1/expedientes` | 409 | 409 | OK |  |
| 15 | Rollback verificado: EXP-2026-0002 no existe | `GET /api/v1/expedientes?personaId=33` | 200 | 200 | OK | codigos=['EXP-2026-0001'] |
| 16 | Resumen de expedientes | `GET /api/v1/expedientes/resumen` | 200 | 200 | OK |  |
| 17 | Cursos filtrados | `GET /api/v1/cursos?q=IS1&sortBy=creditos&dir=desc` | 200 | 200 | OK | cursos=4 |
| 18 | Malla vigente con prerrequisitos | `GET /api/v1/planes-estudio?estado=VIGENTE` | 200 | 200 | OK | planes=1 creditos=21 |
| 19 | Prerrequisitos de IS201 | `GET /api/v1/cursos/5/prerrequisitos` | 200 | 200 | OK | prerrequisitos=IS101 |
| 20 | Seccion con aula ocupada (409) | `POST /api/v1/secciones` | 409 | 409 | OK |  |
| 21 | Seccion con docente ocupado (409) | `POST /api/v1/secciones` | 409 | 409 | OK |  |
| 22 | Secciones con vacantes del periodo | `GET /api/v1/secciones?periodoId=4&conVacantes=true` | 200 | 200 | OK | secciones=6 |
| 23 | Carga docente | `GET /api/v1/docentes/4/carga?periodoId=4` | 200 | 200 | OK | secciones=3 horas=9.0 |
| 24 | Matricula Ana (IS101, IS102, IS103) con cargo | `POST /api/v1/matriculas` | 201 | 201 | OK | creditos=11 cargo=CRG-20261024131037369-4161 monto=700.0 traceId=demo-matricula-ana |
| 25 | Matricula duplicada (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 26 | Cruce de horario IS101/IS104 (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 27 | Prerrequisito no cumplido: Rosa en IS201 (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 28 | Prerrequisito cumplido: Luis en IS201 + IS103 | `POST /api/v1/matriculas` | 201 | 201 | OK | cargo=500.0 en 3 cuotas |
| 29 | IS103 ya llena (2/2): Rosa con IS102 + IS103 (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 30 | Rollback: IS102 no quedo reservada para Rosa | `GET /api/v1/secciones/1002` | 200 | 200 | OK | cupos IS102=1 (antes 0+1 de Ana) |
| 31 | Fecha fuera del periodo (409) | `POST /api/v1/matriculas` | 409 | 409 | OK |  |
| 32 | Matricula sin detalles (400) | `POST /api/v1/matriculas` | 400 | 400 | OK |  |
| 33 | Estudiante inexistente (404) | `POST /api/v1/matriculas` | 404 | 404 | OK |  |
| 34 | Busqueda paginada de matriculas | `GET /api/v1/matriculas?periodoId=4&sortBy=fecha&direction=desc&size=5` | 200 | 200 | OK | total=9 |
| 35 | Reporte agregado de matriculas | `GET /api/v1/matriculas/reporte?periodoId=4` | 200 | 200 | OK | matriculas=9 detalles=14 |
| 36 | Ocupacion de secciones (reporte) | `GET /api/v1/secciones/resumen?periodoId=4` | 200 | 200 | OK |  |
| 37 | Cargo de Ana con cuotas | `GET /api/v1/finanzas/cargos/7` | 200 | 200 | OK | cuotas=140,140,140,140,140 |
| 38 | Pago parcial | `POST /api/v1/finanzas/cargos/7/pagos` | 201 | 201 | OK | estado=PAGADO_PARCIAL saldo=500.0 |
| 39 | Pago mayor al saldo (409) | `POST /api/v1/finanzas/cargos/7/pagos` | 409 | 409 | OK |  |
| 40 | Saldo del estudiante | `GET /api/v1/finanzas/cargos/estudiante/1001/saldo` | 200 | 200 | OK | deuda=700 pagado=200 saldo=500 |
| 41 | Anular matricula con pagos (409) | `PATCH /api/v1/matriculas/93/anular` | 409 | 409 | OK |  |
| 42 | Cargo idempotente (no duplica) | `POST /api/v1/matriculas/93/cargo` | 200 | 200 | OK | mismo cargo=True |
| 43 | Cargo de matricula previa sin creditos (409) | `POST /api/v1/matriculas/1/cargo` | 409 | 409 | OK |  |
| 44 | Resumen financiero por estado | `GET /api/v1/finanzas/cargos/resumen?periodoId=4` | 200 | 200 | OK |  |
| 45 | CORS origen permitido 4200 | `OPTIONS /api/v1/cursos` | 200 | 200 | OK | Access-Control-Allow-Origin=http://localhost:4200 |
| 46 | CORS origen no permitido 5500 (403) | `OPTIONS /api/v1/cursos` | 403 | 403 | OK |  |

Total: 46 casos, 46 OK, 0 fallas
