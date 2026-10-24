# Arquitectura — LAMB Academic Backend

## 1. Visión general

Un **único proyecto Maven** (`backend/pom.xml`, sin reactor) y un **único `.jar`**: monolito modular
Spring Boot 4.0.8 / Java 21, organizado por módulos de negocio que Spring Modulith 2.0.8 verifica
automáticamente (`ModularityTests`). Persistencia real en Oracle (esquema `LAMB_APP`) con
JPA/Hibernate y `ddl-auto: validate`: el esquema lo crean los scripts revisados de `infra/oracle/`,
Hibernate solo comprueba que las entidades coinciden con las tablas.

```
pe.edu.upeu.lambacademic
├── LambAcademicApplication · OpenApiConfig · CorsConfig · HelloController   (raíz compartida)
├── exception/      módulo OPEN: ResourceNotFound (404), ReglaNegocio (409), ErrorResponse, GlobalExceptionHandler
├── filter/         CorrelationIdFilter (X-Trace-ID → MDC → logs)
├── compartido/     módulo OPEN: convertidores JPA (Boolean↔NUMBER(1), LocalTime↔"HH:mm")
├── baseuniversal/  institucional · persona · alumno · expediente
├── coreacademico/  curso · seccion (+port) · plan · docente
├── matricula/      matricula (+port, adapter) · periodo (+adapter)
└── finanzas/       concepto · cargo (+port)
```

Dentro de cada submódulo: `controller → service → repository → entity`, con `dto` y `mapper`
(MapStruct). El controller nunca usa el repositorio; ninguna entidad se serializa (siempre DTO).

## 2. Módulos y dependencias

```
                ┌──────────────┐
                │  matricula   │  (registro cabecera-detalle, periodo)
                └──┬────┬────┬─┘
        usa API    │    │    │   implementa puertos de Core y Finanzas (periodo)
          ┌────────┘    │    └─────────┐
          ▼             ▼              ▼
  ┌──────────────┐ ┌──────────┐ ┌──────────────┐
  │coreacademico │ │ finanzas │ │baseuniversal │
  └──────┬───────┘ └────┬─────┘ └──────────────┘
         └──────────────┴────────────▲
                    usan API de baseuniversal
```

Grafo acíclico verificado por Modulith (diagrama generado en `backend/target/spring-modulith-docs/components.puml`
al ejecutar `ModularityTests`). APIs públicas expuestas con `@NamedInterface`:

| Módulo | Interfaces expuestas | Consumidor |
|---|---|---|
| baseuniversal | `alumno.service`/`alumno.dto` (`AlumnoService.buscarEstudiante → EstudianteResumen`), `persona.service`/`dto` (`obtenerDocente`, `listarDocentes`), `institucional.service`/`dto` (`obtenerEscuela`) | Matrícula, Finanzas, Core |
| coreacademico | `seccion.service`/`dto` (`obtenerParaMatricula`, `reservarCupo`, `liberarCupo`), `seccion.port` (`PeriodoAcademicoPort`), `plan.service` (`prerrequisitosDeCurso`), `curso.dto` | Matrícula |
| finanzas | `cargo.service`/`dto` (`generarCargoMatricula`, `anularCargoDeMatricula`), `cargo.port` (`PeriodoFinancieroPort`) | Matrícula |

**Inversión de dependencias para el Periodo.** El Periodo pertenece a Matrícula (trabajo original),
pero Core (secciones) y Finanzas (cargos) necesitan validarlo, y Matrícula ya depende de ambos.
Para no crear un ciclo, Core declara `PeriodoAcademicoPort` y Finanzas `PeriodoFinancieroPort`;
Matrícula los implementa (`PeriodoAcademicoAdapter`, `PeriodoFinancieroAdapter`).

**Puertos originales de Matrícula preservados.** `EstudianteConsultaPort` y `SeccionConsultaPort`
siguen siendo el contrato del servicio; sus adaptadores pasaron de simulados a reales. Se agregaron
`PrerrequisitoConsultaPort` y `CargoFinancieroPort` con el mismo patrón.

## 3. Modelo de datos (Oracle, esquema `LAMB_APP`)

| Módulo | Tablas | Relaciones |
|---|---|---|
| Base Universal | `SEDE` → `FACULTAD` → `ESCUELA`; `PERSONA` (→ESCUELA); `ALUMNOS` (1:1 PERSONA); `EXPEDIENTES` (→PERSONA) → `DOCUMENTOS_ADJUNTOS` | FK físicas dentro del módulo |
| Core Académico | `CURSO`; `PLAN_ESTUDIO` → `DETALLE_PLAN_ESTUDIO` (→CURSO, prerrequisito →CURSO); `SECCIONES` (→CURSO, →PERIODOS, →PERSONA docente) → `HORARIOS_AULA` | FK físicas; en Java, ids `Long` hacia otros módulos |
| Matrícula | `PERIODOS`; `MATRICULAS` → `DETALLE_MATRICULA` (con `RESULTADO` del curso, V4) | Sin FK hacia ALUMNOS/SECCIONES (registro histórico, criterio S4 Tabla 2) |
| Finanzas | `FIN_CONCEPTOS_PAGO`; `FIN_CARGOS` → `FIN_DETALLE_CARGOS`, `FIN_PAGOS` | Sin FK hacia estudiante/periodo/matrícula (documento histórico) |

Restricciones relevantes: `UK_MATRICULA_ACTIVA (CLAVE_ACTIVA)` (una matrícula activa por estudiante y
periodo), `UK_DET_MATRICULA_SECCION`, `CK_SECCIONES_CUPOS (0 ≤ CUPOS_OCUPADOS ≤ LIMITE_VACANTES)`,
`UX_FIN_CARGOS_ORIGEN` (índice único funcional: un cargo por proceso+referencia),
`CK_FIN_CARGOS_SALDO`, `CK_FIN_DETALLE_PAGADO`, `CK_HORARIOS_*` (formato `HH:mm`, inicio < fin).
Las identidades de `ALUMNOS` y `SECCIONES` inician en 1001 para no colisionar con los ids de prueba
que ya existían en `MATRICULAS`/`DETALLE_MATRICULA`.

## 4. Operaciones transaccionales cabecera-detalle

| Operación | Cabecera / detalle | Reglas | Transacción |
|---|---|---|---|
| `POST /api/v1/matriculas` | Matrícula / DetalleMatrícula | periodo existente, ACTIVO y fecha dentro; estudiante existente y habilitado; una matrícula activa por periodo; sección existente, del periodo, abierta, con cupo; sin sección ni curso repetido; prerrequisitos **aprobados** en un periodo anterior; sin cruce de horarios | Una sola `@Transactional`: INSERT cabecera+detalles → `UPDATE` atómico de cupos (orden ascendente) → cargo financiero. Cualquier excepción revierte todo. |
| `POST /api/v1/planes-estudio` | PlanEstudio / DetallePlanEstudio | curso no repetido, 6–24 créditos por ciclo, prerrequisito del mismo plan y ciclo anterior, electivo no es prerrequisito | `@Transactional` |
| `POST /api/v1/secciones` | Sección / HorarioAula | curso activo, periodo no cerrado, docente válido, grupo único, franja 07:00–22:00, sin cruces internos, de aula ni de docente | `@Transactional` |
| `POST /api/v1/expedientes` | Expediente / DocumentoAdjunto | persona activa, código único, extensión coherente con el tipo, archivo no repetido, 1–20 documentos | `@Transactional` |
| Cargo de matrícula / pago | Cargo / DetalleCargo (+Pago) | monto desde conceptos, cuotas exactas, idempotencia, pago ≤ saldo, aplicación por orden, anulación sin pagos | `REQUIRED` (se une a la matrícula); `@Version` en cargos |

**Concurrencia de cupos.** `SeccionRepository.reservarCupo` ejecuta
`UPDATE SECCIONES SET CUPOS_OCUPADOS = CUPOS_OCUPADOS + 1 WHERE ID = ? AND ESTADO = 'ABIERTA' AND CUPOS_OCUPADOS < LIMITE_VACANTES`.
Oracle bloquea la fila hasta el commit; la segunda transacción reevalúa la condición y no actualiza
filas si el cupo se agotó → `ReglaNegocioException` → rollback. Probado con dos hilos reales.

## 5. Contrato REST transversal

- Prefijo versionado `/api/v1/...`; OpenAPI en `/v3/api-docs` y Swagger UI en `/swagger-ui.html`.
- Errores con formato único `ErrorResponse {timestamp, status, error, message, path, traceId, errores}`:
  400 validación/JSON mal formado/tipos, 404 inexistente, 409 regla de negocio o restricción Oracle,
  405 método, 500 sin detalles internos.
- `X-Trace-ID` en cada respuesta (se respeta el enviado por el cliente) y en cada línea de log
  (`backend/logs/lamb-academic.log`, rotación diaria, 7 días).
- CORS por propiedad `lamb.cors.allowed-origins` (lista separada por comas, nunca `*`), aplicado a `/api/**`
  y exponiendo `X-Trace-ID`.
- Filtros combinados con `(:param IS NULL OR …)` en una sola consulta, `Sort` con lista blanca de
  campos, proyecciones con `SELECT new …` y agregaciones (`/resumen`, `/reporte`).
- Paginación (`GET /api/v1/matriculas`) en Oracle (sin `JOIN FETCH` de colección; `@BatchSize`) y JSON
  estable `{content, page{size, number, totalElements, totalPages}}` (`PaginacionConfig`, `VIA_DTO`).
- Contratos que cambiaron respecto a cada aporte: `CONTRATOS_REST.md`.

## 6. Configuración y secretos

`application.yml` (base, `spring.config.import: optional:file:.env[.properties]`) y
`application-dev.yml` (ambiente DEV). Las credenciales se leen de `backend/.env` y `infra/.env`,
ambos ignorados por Git (plantillas `*.env.example`). Propiedades propias:

| Propiedad | Valor DEV | Uso |
|---|---|---|
| `lamb.cors.allowed-origins` | `http://localhost:4200` | Orígenes del frontend |
| `lamb.matricula.prerrequisitos.politica` | `APROBADO` | `APROBADO` (exige resultado APROBADO), `CURSADO_TEMPORAL` (transición, identificada en el mensaje) o `NINGUNA` |
| `lamb.matricula.cuotas-por-defecto` | `5` | Cuotas del cargo si la solicitud no las indica |

## 7. Decisiones registradas

1. Persona única con el modelo de LP2_G1 (superconjunto del de LP2_L).
2. Curso único en Core Académico.
3. Docente = Persona `DOCENTE` (ningún aporte tenía entidad Docente); la carga se deriva de secciones.
4. Prerrequisito = columna `ID_PRERREQUISITO` del detalle de malla (modelo de LP2_G1).
5. Un solo esquema (`LAMB_APP`), sin crear esquemas por módulo: es la base existente con datos reales.
6. Sin Flyway (las guías no lo usan en LP2): scripts SQL numerados, aditivos y con reversión aparte.
7. JWT de LP2_L pospuesto a U2 (S10 según las guías).
8. `Boolean` mapeado a `NUMBER(1)` con convertidor: Oracle 23ai espera `BOOLEAN` nativo y la
   validación fallaba (el código de LP2_G1 no habría arrancado con `validate`).
9. Prerrequisitos: un curso solo acredita si su detalle de matrícula tiene resultado APROBADO
   (registrado con el periodo CERRADO). No hay módulo de notas: se guarda solo el resultado final.
