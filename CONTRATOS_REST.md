# Contratos REST: cambios respecto a los aportes originales e impacto en el frontend

Fuente de verdad del contrato vigente: OpenAPI en `/v3/api-docs` (Swagger UI `/swagger-ui.html`).
Este documento compara contra el código original de cada aporte (ZIP de los compañeros y
`main` para Matrícula) para que el frontend de la Unidad II no consuma contratos antiguos.

## 0. Cambios transversales (afectan a todos los módulos)

| Tema | Antes | Ahora | Impacto en el frontend |
|---|---|---|---|
| Formato de error | Distinto en cada aporte: `{"mensaje"}` (Matrícula), `{"error"}` o `{campo: msg}` (LP2_L, Finanzas), `{timestamp,status,error,message}` (lambacademic), `{timestamp,status,error,message,path}` (LP2_G1) | Único `ErrorResponse {timestamp, status, error, message, path, traceId, errores?}`; `errores` = mapa campo→mensaje solo en 400 de validación | Un solo interceptor HTTP: mostrar `message`; pintar `errores` junto a cada campo; registrar `traceId` |
| Códigos HTTP | Muchas reglas respondían 400 (Matrícula, LP2_L, Finanzas) o 500 (recursos inexistentes) | 400 validación/JSON/tipos · 404 inexistente · 409 regla de negocio o restricción Oracle · 405 método · 500 sin detalles | Distinguir 409 (mensaje al usuario) de 400 (errores de formulario) |
| CORS | `@CrossOrigin` por controller o `allowedOrigins` fijos en código | Global para `/api/**`, por propiedad `lamb.cors.allowed-origins` (por defecto `http://localhost:4200`), expone `X-Trace-ID` | El frontend en 4200 funciona sin cambios; otros orígenes se agregan en `application-dev.yml` |
| Trazabilidad | No existía | Cabecera `X-Trace-ID` en toda respuesta (si el cliente la envía, se respeta) | Opcional: enviar un id por acción del usuario para soporte |
| Fechas y horas | Horas como texto libre (`"8:00"`) | `LocalDate` ISO (`2026-10-24`), `LocalDateTime` ISO, horas `"HH:mm"` | Formatear con ceros (`08:00`) |
| Enumerados | Textos libres (`estado`, `procesoOrigen`, `diaSemana`, `periodo`) | Enumerados validados (ver cada módulo); un valor no válido → 400 | Usar listas cerradas (select) |

## 1. Base Universal — aporte LP2_L (Persona, Alumno, Expediente, DocumentoAdjunto)

| Endpoint original | Contrato original | Contrato actual | Impacto |
|---|---|---|---|
| `GET /api/v1/personas` | Lista de la **entidad** `{id, dni, nombres, apellidos, email}` | `PersonaResponse {id, tipoPersona, tipoDocumento, numeroDocumento, nombres, apellidoPaterno, apellidoMaterno, nombreCompleto, email, telefono, direccion, fechaNacimiento, idEscuela, escuelaNombre, activo}`; filtros `tipoPersona, activo, q, idEscuela, sortBy, dir` | Reemplazar `dni`→`numeroDocumento`, `apellidos`→`apellidoPaterno`/`apellidoMaterno` |
| `GET /api/v1/personas/buscar?filtro=` | Lista por nombre o DNI | **Eliminado** → `GET /api/v1/personas?q=` (documento, nombres, apellidos, correo) | Cambiar la URL |
| `GET /api/v1/personas/paginado` | `Page<Persona>` | **Eliminado** (la guía S5 usa filtros + orden sin paginación) | Paginar en el cliente si hace falta |
| `POST /api/v1/personas` | `{dni (8), nombres, apellidos, email}` | `{tipoPersona*, tipoDocumento*, numeroDocumento* (8–12 dígitos; DNI 8, RUC 11), nombres*, apellidoPaterno*, apellidoMaterno, email, telefono, direccion, fechaNacimiento, idEscuela}`; 409 si el documento existe | Formulario nuevo |
| `DELETE /api/v1/personas/{id}` | Borrado físico | Borrado **lógico** (`activo=false`), 204 | Filtrar por `activo` en listados |
| — | — | Nuevos: `GET /{id}`, `PUT /{id}`, `GET /documento/{tipo}/{numero}`, `GET /resumen`, `GET /resumen/escuela` | — |
| `GET /api/v1/alumnos` | `AlumnoDto {id, codigoEstudiante, estado, personaId}` | `AlumnoResponse {id, codigoEstudiante, estado (ACTIVO/SUSPENDIDO/RETIRADO/EGRESADO), fechaIngreso, habilitado, persona {id, numeroDocumento, nombreCompleto, email, idEscuela, escuelaNombre}}`; filtros `estado, idEscuela, q` | `personaId` pasa a `persona.id` |
| `POST /api/v1/alumnos` | `{codigoEstudiante, estado, personaId}` sin validar | `{personaId*, codigoEstudiante* (A-Z0-9-, 4–20), fechaIngreso*}`; el estado inicial es ACTIVO; la persona debe ser de tipo ALUMNO (409) | Quitar `estado`; agregar `fechaIngreso` |
| — | — | Nuevos: `GET /{id}`, `PUT /{id}`, `PATCH /{id}/estado`, `GET /resumen` | — |
| `GET/POST /api/v1/expedientes` y `POST /api/v1/expedientes/con-documentos` | Entidad expuesta; dos DTO distintos (`codigoExpediente/observacion` y `codigoTramite/asunto`) | Un solo `POST /api/v1/expedientes {codigoTramite*, asunto*, personaId*, documentos* (1–20) [{nombre*, nombreArchivo*, tipoDocumento* (PDF/JPG/PNG/DOCX), rutaArchivo*}]}` → `ExpedienteResponse` con `persona` y `documentos`. `GET` devuelve `ExpedienteResumen` (sin documentos) con filtros `personaId, estado, desde, hasta` | Usar el formato `codigoTramite/asunto`; la extensión del archivo debe coincidir con el tipo |
| `GET/POST /api/v1/documento-adjunto` | Lista global de la entidad; alta suelta | **Reemplazado** por `GET/POST /api/v1/expedientes/{id}/documentos` | Cambiar URL; el documento siempre pertenece a un expediente |
| — | — | Nuevos: `GET /{id}`, `PATCH /{id}/estado` (flujo REGISTRADO→EN_REVISION→OBSERVADO/APROBADO→ARCHIVADO), `GET /resumen` | — |

## 2. Base Universal — aporte LP2_G1 (institucional) y Core Académico — Currículo

| Endpoint | Cambio | Impacto |
|---|---|---|
| `GET /api/v1/personas/resumen`, `/resumen/escuela` | `Map<String, Long>` → lista `[{categoria, cantidad}]` | Iterar la lista en lugar de un objeto |
| `POST /api/v1/personas` | `nombres` y `apellidoPaterno` pasan a obligatorios | Marcar como requeridos |
| `/api/v1/facultades`, `/api/v1/escuelas` | Nuevos `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (lógico); `DELETE /sedes/{id}` responde 409 si la sede tiene facultades activas | — |
| `GET /api/v1/hello` | LP2_G1 devolvía un mapa JSON; se conserva el del proyecto principal (texto `Hola LAMB Academic`) | Solo para verificación |
| `POST /api/v1/planes-estudio` | Igual, más reglas: el prerrequisito debe pertenecer al mismo plan y estar en un ciclo anterior; curso inactivo → 409; `codigo` hasta 25 caracteres | Validar en el formulario |
| `PlanEstudioResponse.detalles[]` | Agrega `prerrequisitoCodigo` | — |
| `GET /api/v1/planes-estudio/resumen` | `{escuela, estado, cantidadPlanes, totalCreditos}` → agrega `idEscuela` al inicio | — |
| — | Nuevo `GET /api/v1/cursos/{id}/prerrequisitos?idEscuela=` | — |
| — | Nuevos `GET /api/v1/docentes`, `GET /api/v1/docentes/{id}/carga?periodoId=` | — |

## 3. Core Académico — aporte lambacademic (Curso, Sección, HorarioAula)

| Endpoint | Contrato original | Contrato actual | Impacto |
|---|---|---|---|
| `GET /api/v1/cursos` | `{id, codigo, nombre, creditos, estado (texto)}` | `{id, codigo, nombre, creditos, horasTeoricas, horasPracticas, activo (boolean)}`; filtros `activo, creditos, q`, orden | `estado` → `activo` |
| `POST/PUT/DELETE /api/v1/cursos` | No existían (solo lectura) | CRUD completo (`DELETE` lógico) | — |
| `POST /api/v1/secciones` | `{periodo (texto), grupo, limiteVacantes, cursoId, horarios[{diaSemana (texto), horaInicio (texto), horaFin, aula}]}` | `{cursoId*, periodoId* (id de PERIODOS), grupo* (A-Z0-9, 1–10), docenteId, limiteVacantes* (1–200), horarios* (1–10) [{diaSemana* LUNES…DOMINGO, horaInicio* "HH:mm", horaFin*, aula*}]}`; 409 por cruce de aula, de docente, periodo cerrado o grupo repetido; franja 07:00–22:00 | Usar `periodoId` y enumerado de días |
| `SeccionResponse` | `{id, periodo, grupo, limiteVacantes, curso (CursoResponse), horarios}` | `{id, curso {id, codigo, nombre, creditos}, periodoId, grupo, docenteId, docenteNombre, limiteVacantes, cuposOcupados, vacantesDisponibles, estado (ABIERTA/CERRADA/CANCELADA), horarios [{id, diaSemana, horaInicio, horaFin, aula}]}` | Mostrar vacantes disponibles |
| `GET /api/v1/secciones` | Lista completa | Filtros `periodoId, cursoId, docenteId, estado, conVacantes`, orden | — |
| — | — | Nuevos: `PUT /{id}` (docente, límite ≥ ocupados), `PATCH /{id}/estado`, `GET /resumen?periodoId=` | — |

## 4. Finanzas del Estudiante — aporte Frinanzas-lamb

| Endpoint | Contrato original | Contrato actual | Impacto |
|---|---|---|---|
| `POST /api/v1/finanzas/cargos` | `{estudianteId, procesoOrigen (texto), referenciaId, periodoId, totalCreditos, numeroCuotas, observacion}`; monto con tarifas fijas | `procesoOrigen` enumerado `TRAMITE/OTRO` (**`MATRICULA` → 409**: ese cargo lo genera la matrícula); `numeroCuotas` 1–12; `conceptos` opcional (códigos de `FIN_CONCEPTOS_PAGO`); estudiante y periodo validados (404); referencia repetida → 409 | No registrar cargos de matrícula desde el frontend |
| `CargoResponse` | `createdAt`; cuotas `DetalleCargoResponse` con `estado` texto | `fechaEmision` (antes `createdAt`), agrega `totalCreditos`; cuotas `CuotaResponse` con `estado` PENDIENTE/PAGADA_PARCIAL/PAGADA/ANULADA; las cuotas suman exactamente el total | Renombrar `createdAt` |
| `GET /estudiante/{id}/saldo` | `deudaTotal` incluía cargos anulados; 200 aunque el estudiante no existiera | Excluye anulados; agrega `cuotasVencidas`; 404 si el estudiante no existe | — |
| `PATCH /{id}/anular` | Siempre anulaba | 409 si el cargo tiene pagos | Informar al usuario |
| Errores | Toda `RuntimeException` → 400 | 404/409 según el caso | — |
| — | — | Nuevos: `GET /api/v1/finanzas/cargos` (filtros), `POST/GET /{id}/pagos`, `GET /resumen`, CRUD `/api/v1/finanzas/conceptos` | — |

## 5. Matrícula — proyecto principal (trabajo original)

| Endpoint | Contrato original (`main`) | Contrato actual | Impacto |
|---|---|---|---|
| `POST /api/v1/matriculas` | `{matricula {estudianteId, periodoId, fecha, estado}, detalles [{seccionId}]}` | Igual; agrega `matricula.numeroCuotas` (opcional, 1–12); `detalles` obligatorio con 1–8 elementos | Compatible hacia atrás salvo listas vacías |
| Respuesta | `{id, estudianteId, periodoId, fecha, estado, detalles [{id, seccionId}]}` | Agrega `detalles[].cursoId`, `detalles[].creditos`, `detalles[].resultado`, `totalCreditos`, `cargo {cargoId, numeroCargo, montoTotal, numeroCuotas, estado}`. `cargo` se informa en **todas** las respuestas (registro, `GET /{id}`, listado, anulación, resultado); es `null` solo si la matrícula no tiene cargo (p. ej. las previas a la integración) | Campos nuevos: ignorarlos es seguro |
| Errores de las reglas | 400 `{"mensaje"}` (sección inexistente → 500) | 404 periodo/estudiante/sección; 409 periodo no activo, fecha fuera, estudiante no habilitado, matrícula duplicada, sección repetida, de otro periodo o no abierta, sin cupos, curso repetido, prerrequisito, cruce; formato `ErrorResponse` (`message`) | Cambiar `mensaje` → `message` y no asumir 400 |
| `GET /api/v1/matriculas` | `PageImpl` serializado tal cual (`content, pageable, totalElements, totalPages, last, size, number, sort, first, numberOfElements, empty`); `sortBy` libre (campo inválido → 500); `direction` inválida → 500 | **`{content: [...], page: {size, number, totalElements, totalPages}}`** (Spring Data `VIA_DTO`, contrato estable); `sortBy` en lista blanca (`fecha, id, estado, estudianteId, periodoId`); `size` 1–100 | Leer `page.totalElements` en lugar de `totalElements` |
| `GET /resumen`, `GET /reporte` | — | Sin cambios | — |
| — | — | Nuevos: `GET /{id}`, `PATCH /{id}/anular`, `POST /{id}/cargo`, `PATCH /{id}/detalles/{detalleId}/resultado` | — |
| `POST/PUT /api/v1/periodos` | `estado` libre (≤20) | `estado` ∈ `PLANIFICADO, ACTIVO, CERRADO`; 409 por código duplicado o fecha fin ≤ inicio | Usar lista cerrada |
| `GET/PUT/DELETE /api/v1/periodos/{id}` inexistente | 500 (`RuntimeException`) | 404; `DELETE` con matrículas → 409 | — |

## 6. Recomendaciones para el frontend (Unidad II)

1. Generar los servicios desde `/v3/api-docs` (o tipar a mano con este documento) y no reutilizar los servicios Angular de los ZIP: apuntan a contratos anteriores.
2. Interceptor HTTP único para `ErrorResponse` y `X-Trace-ID`.
3. Selects con los enumerados: `tipoPersona`, `tipoDocumento`, `EstadoAlumno`, `TipoArchivo`, `DiaSemana`, `EstadoSeccion`, estados de periodo, `ProcesoOrigen` (`TRAMITE`, `OTRO`), `MedioPago`, `ResultadoCurso`.
4. El `estudianteId` de Matrícula y Finanzas es el **id de ALUMNOS** (no el de PERSONA).
5. ~~Seguridad JWT pendiente~~ → **Integrada en U2 con Keycloak** (ver §7).

## 7. Unidad II: seguridad y cambios de contrato

| Cambio | Detalle | Impacto en clientes |
|---|---|---|
| Autenticación | Todas las rutas `/api/**` (salvo `/api/v1/hello`) exigen `Authorization: Bearer <token de Keycloak>` | Sin token → **401** con `WWW-Authenticate: Bearer` y el cuerpo `ErrorResponse` |
| Autorización por rol | Matriz completa en `docs/SEGURIDAD_KEYCLOAK.md` §5 | Rol insuficiente → **403** `"Tu rol no tiene permiso para esta operación"` |
| Filtro por dueño (ESTUDIANTE) | `GET /matriculas` se limita a sus matrículas; `GET /matriculas/{id}`, `POST /matriculas`, `GET /finanzas/cargos/{id}`, `/{id}/pagos`, `/estudiante/{id}`, `/estudiante/{id}/saldo` exigen que el recurso sea suyo (claim `alumno_id`) | Recurso ajeno → **403** con el motivo |
| Nuevo | `DELETE /api/v1/secciones/{id}` → 204 | 404 si no existe; 409 si tiene matrículas (aunque estén anuladas), cupos ocupados o el periodo está cerrado |
| Nuevo | `GET /api/v1/auth/perfil` → `{sub, usuario, nombre, email, roles, alumnoId}` | Lectura del token, sin base de datos |
| CORS | Ahora lo aplica Spring Security (preflight sin token); orígenes en `lamb.cors.allowed-origins` | Sin cambio para `http://localhost:4200` |
| Trazabilidad | `X-Trace-ID` también en las respuestas 401/403 | — |
