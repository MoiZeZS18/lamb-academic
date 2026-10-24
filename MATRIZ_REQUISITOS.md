# Matriz de requisitos — LAMB Academic Backend (Unidad I)

Leyenda de estado: **Cumple** (implementado y probado) · **Parcial** · **Pendiente**.
Columna «Origen»: **Docente** = exigido por las guías S1–S6; **Integración** = mejora técnica
necesaria para unir los módulos (no es requisito del docente); **Usuario** = pedido en la misión.

## 1. Requisitos obligatorios de las guías (S1–S6)

| # | Requisito | Guía | Evidencia en el código | Prueba | Estado |
|---|---|---|---|---|---|
| D1 | Un solo proyecto Maven, Java 21, Spring Boot 4.0.x | S1 2.2, S6 P1 | `backend/pom.xml` (Boot 4.0.8, Java 21) | build Maven | Cumple |
| D2 | Monolito modular verificado con Spring Modulith (`ModularityTests` en verde) | S1 3.4.4, S4 3.11, S6 | `ModularityTests` (verify + Documenter); violación provocada detectada | `ModularityTests` 3/3; prueba negativa | Cumple |
| D3 | Módulos como paquetes directos; comunicación solo por servicios públicos (`@NamedInterface`) | S1 2.1, S4 3.2 | `package-info.java` en `*.service`, `*.dto`, `*.port` | `ModularityTests` | Cumple |
| D4 | `exception` transversal `Type.OPEN` | S4 3.5 | `exception/package-info.java` | `ModularityTests` | Cumple |
| D5 | Configuración por ambientes (`application.yml` + `application-dev.yml`) sin secretos en el repo | S1 2.3, 4.4 | `.env` ignorado, `*.env.example` | revisión de commits | Cumple |
| D6 | ORM JPA/Hibernate, driver Oracle, `ddl-auto: validate` | S1 3.2.3 | `application-dev.yml` | `LambAcademicApplicationTests` (valida 19 tablas) | Cumple |
| D7 | Endpoint de verificación y Actuator `health` con `db` UP | S1 3.2.4 | `/api/v1/hello`, `/actuator/health` | E2E #1, #3 | Cumple |
| D8 | Contrato versionado `/api/v1` y OpenAPI/Swagger | S1 2.5, 3.2.5 | `OpenApiConfig`, `@Tag/@Operation` | E2E #2 (`/v3/api-docs`) | Cumple |
| D9 | Entity, Repository, Service, Controller, DTO y Mapper por recurso | S1–S2 | todos los submódulos; mappers MapStruct | unitarias/web | Cumple |
| D10 | DTO de entrada distinto del de salida; entidades nunca expuestas | S2 2.2, S6 P3 | `*Request`/`*Response` | `PersonaControllerTest` | Cumple |
| D11 | Validación de entrada con Bean Validation y `@Valid` → 400 | S2 2.3 | anotaciones en `*Request` | `MatriculaControllerTest`, `PersonaControllerTest`, E2E #7, #32 | Cumple |
| D12 | `GlobalExceptionHandler`: 400/404/409/500 | S1 3.8, S2 3.2.1, S4 3.5 | `exception/GlobalExceptionHandler` | web + E2E | Cumple |
| D13 | Logs y trazabilidad por petición (`CorrelationIdFilter`, `traceId` en logs) | S2 3.2.2 | `filter/CorrelationIdFilter`, `logback-spring.xml` | `MatriculaControllerTest` (X-Trace-ID); log real | Cumple |
| D14 | CRUD REST completo de una entidad principal | S2 | Persona, Alumno, Curso, Sede/Facultad/Escuela, Periodo, ConceptoPago | E2E #6–#12 | Cumple |
| D15 | Objetos relacionados: asociación, DTO relacionado, navegación controlada, sin ciclos | S3 | `PersonaResumen`, `CursoResumen`, `ArbolResponse`; relaciones unidireccionales | E2E #5, #18 | Cumple |
| D16 | Validación de referencias en el service (404 limpio, no error de FK) | S3 2.3 | `buscarOFallar`, `cursoActivo`, `obtenerEscuela` | unitarias | Cumple |
| D17 | Cabecera-detalle con DTO compuesto, cálculos y regla de negocio real | S4 | Matrícula, Malla, Sección, Expediente, Cargo | unitarias + integración | Cumple |
| D18 | `@Transactional`, commit y **rollback demostrado** | S4 2.5, S6 sec. 3 | `MatriculaService.registrar` | `MatriculaRollbackOracleIntegrationTest`; E2E #14–15, #29–30 | Cumple |
| D19 | Comunicación entre módulos dentro de la misma transacción | S4 2.5 | Matrícula → Core (cupos) → Finanzas (cargo) | integración | Cumple |
| D20 | Filtros combinados `(:param IS NULL OR …)`, ordenamiento `Sort` | S5 2.2–2.3 | repositorios `buscar(...)` | E2E #10, #17, #22, #34 | Cumple |
| D21 | Proyecciones y agregaciones (`/resumen`) | S5 2.4–2.5 | `ExpedienteResumen`, `CargoResumenResponse`, `*Reporte` | E2E #16, #35, #36, #44 | Cumple |
| D22 | CORS configurable por propiedad, sin `*`, varios orígenes | S5 3.7 | `CorsConfig`, `lamb.cors.allowed-origins` | `CorsConfigTest`; E2E #45–46 | Cumple |
| D23 | Pruebas automatizadas (incluye controller con `@WebMvcTest`) | S2 3.9 | `src/test/**` | 83 pruebas | Cumple |
| D24 | Persistencia real en Oracle (esquema de la BD del equipo) | S1, S6 | esquema `LAMB_APP` | integración + E2E | Cumple |
| D25 | Producto `lp2-demo.md` (plantilla del docente) | S6 tabla 4 | — | — | **Pendiente académico** (no bloquea la integración; la plantilla no está en el material recibido) |

## 2. Requisitos de la misión por módulo

| # | Requisito | Origen | Implementación | Prueba | Estado |
|---|---|---|---|---|---|
| U1 | Persona, Alumno, Expediente, DocumentoAdjunto + CRUD | Usuario | `baseuniversal` | E2E #6–#16 | Cumple |
| U2 | Transacción Expediente–DocumentoAdjunto | Usuario | `ExpedienteService.registrar` | E2E #13–15 | Cumple |
| U3 | Matrícula consulta estudiantes reales | Usuario | `EstudianteConsultaAdapter` → `AlumnoService` | integración | Cumple |
| U4 | Periodo, Matrícula, DetalleMatrícula, registro transaccional (preservado) | Usuario | `matricula.*` | unitarias + integración | Cumple |
| U5 | Cupos, cruces de horario, validación de estudiante y sección, duplicados | Usuario | `MatriculaService` | 17 unitarias + E2E | Cumple |
| U6 | Prerrequisitos aplicables (aprobado ≠ cursado) | Usuario | `PrerrequisitoConsultaAdapter` + `cursosAcreditadosAntesDe` + resultado por curso (V4) | 4 unitarias de política + integración Oracle + E2E | **Cumple** (sin notas ni actas: solo resultado final; ver `PENDIENTES.md` A1) |
| U7 | Concurrencia al reservar cupos | Usuario | UPDATE atómico + orden ascendente | `soloUnoObtieneElUltimoCupo` (2 hilos, Oracle) | Cumple |
| U8 | ConceptoPago, Cargo, DetalleCuota (DetalleCargo), deuda, cuotas, saldos | Usuario | `finanzas.*` | unitarias + E2E #37–44 | Cumple |
| U9 | Integración Matrícula → cargo; sin duplicados ni errores de redondeo; recuperación | Usuario | misma transacción; `UX_FIN_CARGOS_ORIGEN`; `POST /matriculas/{id}/cargo` | `CalculadoraCargoTest`, E2E #42 | Cumple |
| U10 | Curso, Sección, HorarioAula, programación, cupos, aulas | Usuario | `coreacademico.curso/seccion` | `SeccionServiceTest`, E2E #20–23 | Cumple |
| U11 | PlanEstudio, Prerrequisito, Docente | Usuario | `coreacademico.plan/docente` | `PlanEstudioServiceTest`, E2E #18–19, #23 | Cumple (Docente como Persona `DOCENTE`) |
| U12 | Endpoints listos y documentados para el frontend | Usuario | 85 operaciones en OpenAPI; cambios en `CONTRATOS_REST.md` | E2E | Cumple |
| U13 | No usar `ddl-auto=update`; scripts revisados y reversibles | Usuario | `infra/oracle/V2*`, `V3*` | aplicados con respaldo | Cumple |

## 3. Mejoras técnicas de integración (no exigidas por el docente)

| # | Mejora | Motivo |
|---|---|---|
| I1 | Puertos `PeriodoAcademicoPort` / `PeriodoFinancieroPort` | Evitar ciclo de módulos |
| I2 | `BooleanNumeroConverter` | Oracle 23ai + Hibernate esperan BOOLEAN nativo |
| I3 | `CLAVE_ACTIVA` + `UK_MATRICULA_ACTIVA` | Duplicados seguros ante concurrencia sin tocar datos previos |
| I4 | Índice único funcional `UX_FIN_CARGOS_ORIGEN` | Idempotencia del cargo |
| I5 | `@Version` en `FIN_CARGOS` | Pagos concurrentes |
| I6 | Pagos (`FIN_PAGOS`) | Saldo con significado real y anulación segura |
| I7 | Cruce de aula y de docente al programar secciones | Coherencia de la oferta |
| I8 | Anulación de matrícula (libera cupos, anula cargo) | Consistencia y recuperación |
| I9 | Paginación en Oracle + JSON de página estable (`VIA_DTO`) | Rendimiento y contrato para el frontend |
| I10 | Cargo manual de MATRICULA rechazado | Evitar deuda duplicada |
| I11 | Pruebas HTTP repetibles con autolimpieza | No alterar datos reales |
