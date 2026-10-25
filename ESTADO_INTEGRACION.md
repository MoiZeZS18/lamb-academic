# Estado de la integración — LAMB Academic Backend

> Documento vivo. Registra la auditoría inicial, las decisiones y el estado exacto para
> reanudar el trabajo sin repetirlo. Última actualización: 2026-10-24.

## 1. Auditoría inicial (antes de modificar código)

### 1.1 Git

| Elemento | Estado encontrado |
|---|---|
| Repositorio | `D:\LP2\lamb-academic` (rama `main`, al día con `origin/main`) |
| Commits | `cbbb88a` estructura inicial · `4e5beab` avance final matrícula y S5 |
| Worktrees | Solo el principal |
| Sin seguimiento | `.vscode/`, `infra/compose-dev.yml`, `lamb-academic/` |
| `lamb-academic/` anidado | **Copia exacta** de `backend/`, `infra/` y `README.md` (verificado con `diff -r`). No se modificó ni se borró; queda ignorado por Git. |
| Rama de trabajo | `integracion/backend-unificado` creada desde `main` (los cambios pendientes no se perdieron: eran solo archivos sin seguimiento). |

### 1.2 Docker y Oracle

| Elemento | Valor |
|---|---|
| Contenedor | `lamb-academic-oracle` (`gvenzl/oracle-free:23-slim`, Oracle AI Database 26ai Free 23.26.2), puerto host `1522` |
| Volumen | `lamb-academic-dev_oracle-data` (no se tocó) |
| Otro contenedor activo | `bomerp-oracle` (puerto 1521) — pertenece al demo del docente, no se usa |
| Esquema de la app | `LAMB_APP` (rol `DB_DEVELOPER_ROLE`, cuota ilimitada en `USERS`) |
| Tablas previas | `PERIODOS` (1 fila), `MATRICULAS` (7), `DETALLE_MATRICULA` (9) |
| Respaldo previo | Data Pump `expdp schemas=LAMB_APP` → `infra/backups/lamb_app_pre_integracion_20261024_122636.dmp` (SHA-256 `9bbc5f6d…4864fe`). Verificado con `impdp … sqlfile=` (3 `CREATE TABLE` legibles). |

Datos legados detectados (se conservan sin cambios):

- Las 7 matrículas son del `ESTUDIANTE_ID = 1` en el periodo `4`, estado `INICIADA` (6 duplicadas).
- Los 9 detalles apuntan a secciones `1, 2, 100, 101`, que provenían del adaptador simulado.

### 1.3 Guías del docente (S1–S6)

Requisitos obligatorios extraídos (detalle en `MATRIZ_REQUISITOS.md`): proyecto Maven único,
Java 21, Spring Boot 4.0.x, Spring Modulith con `ModularityTests` en verde, módulos como
paquetes directos bajo la raíz, comunicación entre módulos solo por servicios públicos
(`@NamedInterface`), paquete `exception` abierto (`Type.OPEN`), `ddl-auto: validate` (el
esquema lo crean scripts, no Hibernate; LP2 no usa Flyway), Entity/Repository/Service/
Controller/DTO/Mapper, DTO de entrada distinto del de salida, `GlobalExceptionHandler`
(400/404/409/500), `CorrelationIdFilter` + `logback-spring.xml` con `traceId`, relaciones
unidireccionales sin ciclos, validación de referencias en el service, cabecera-detalle con
`@Transactional` y rollback demostrado, filtros combinados `(:p IS NULL OR …)`, `Sort`,
proyecciones, agregaciones (`/resumen`), CORS por propiedad sin `*`, OpenAPI y pruebas.

### 1.4 Proyecto principal (Matrícula)

- Spring Boot 4.0.8, Modulith 2.0.8, springdoc 3.1.0, ojdbc17, MapStruct 1.6.3, Java 21.
- Compila y su única prueba (`contextLoads`) pasa contra Oracle.
- Módulo `matricula` con `matricula.matricula` y `matricula.periodo`.
- Hallazgos: adaptadores `EstudianteConsultaAdapter` y `SeccionConsultaAdapter` simulados;
  el controller usaba el repositorio directamente; todas las reglas respondían 400;
  sin `ModularityTests`, sin `CorrelationIdFilter`, sin CORS; `System.out.println`;
  la verificación de cupos no era segura ante concurrencia; `PeriodoService` lanzaba
  `RuntimeException` (500) para recursos inexistentes.

### 1.5 Material de los compañeros

| ZIP | Contenido real | Versión | Estado |
|---|---|---|---|
| `LP2_L.zip` → `lamb-upeu-backend` | **Base Universal**: Persona (`dni`, `nombres`, `apellidos`, `email`), Alumno, Expediente, DocumentoAdjunto, JWT | Boot 3.2.5, **Java 17** | **No compila**: `ExpedienteController` llama a `listarExpedientes()` y `guardarExpedienteConDetalle()`, que no existen. Expone entidades en la API, `ddl-auto: update`, errores como 400 genérico. |
| `LP2_G1.zip` → `bomerp-backend` | **Currículo/Institucional** (Cristian): Sede, Facultad, Escuela, Curso, Persona (modelo completo con tipo de persona DOCENTE/ALUMNO), PlanEstudio–DetallePlanEstudio con prerrequisito, especificaciones, reportes; scripts BD2 | Boot 4.0.7, Java 21 | Buena calidad; 27 pruebas. Los scripts BD2 incluyen `DROP TABLE` (no se ejecutaron). |
| `lambacademic.zip` → `lambacademic-backend` | **Core Académico — Oferta**: Curso, Seccion, HorarioAula, `CorrelationIdFilter`, `ModularityTests` | Boot 3.4.3, Java 21 | Funcional pero mínimo: periodo como texto, sin cupos ocupados ni cruces de aula. |
| `Frinanzas-lamb.zip` → `finanzas-backend` | **Finanzas**: Cargo, DetalleCargo (cuotas), EstadoCargo, saldo | Boot 3.4.3, Java 21 | Error de redondeo (las cuotas no suman el total), deuda total incluye anulados, tarifas fijas en código, sin control de duplicados, usuario `system` en una de sus configuraciones. |

No existe en ningún ZIP una entidad `ConceptoPago`, `Docente` ni `Prerrequisito` independiente:
el docente es una `Persona` con `tipoPersona = DOCENTE` y el prerrequisito es una columna del
detalle de la malla (`DETALLE_PLAN_ESTUDIO.ID_PRERREQUISITO`).

### 1.6 Conflictos e incompatibilidades detectados

| Conflicto | Resolución |
|---|---|
| Dos modelos de `Persona` (LP2_L vs LP2_G1) | Se adopta el de LP2_G1 (superconjunto: tipo y número de documento, apellidos separados, escuela). `Alumno` (LP2_L) referencia a esa `Persona`. |
| Dos modelos de `Curso` (LP2_G1 vs lambacademic) | Un solo `Curso` en `coreacademico.curso` (columnas de LP2_G1). |
| Periodo como texto en `Seccion` | `Seccion.periodoId` referencia `PERIODOS` (FK física). |
| Ciclo de dependencias Matrícula ↔ Core/Finanzas por el Periodo | Inversión de dependencias: Core y Finanzas declaran un puerto `PeriodoAcademicoPort`; Matrícula lo implementa. |
| Cinco `GlobalExceptionHandler` | Uno solo en el paquete abierto `exception`. Las excepciones de Matrícula se conservan y heredan de `ResourceNotFoundException` o `ReglaNegocioException`. |
| Versiones Boot 3.2/3.4/4.0, Java 17/21 | Se unifica en Boot 4.0.8 / Java 21 del proyecto principal (Jackson 3 en pruebas, nuevos paquetes de test). |
| JWT de LP2_L | Se pospone a U2 (las guías lo ubican en S10). No se integra. |
| Contraseñas en archivos versionados | Se externalizan a archivos `.env` ignorados por Git. |

## 2. Plan técnico

1. **Infraestructura común**: `exception` (OPEN), `filter/CorrelationIdFilter`, `logback-spring.xml`, `CorsConfig` por propiedad, `ModularityTests`, credenciales fuera de Git.
2. **Migración Oracle revisada** (`infra/oracle/`): scripts aditivos (solo `CREATE` y `ALTER … ADD`), script de reversión separado (no ejecutado), datos de demostración opcionales. Respaldo previo verificado.
3. **Base Universal** (`baseuniversal`): institucional, persona, alumno, expediente (cabecera-detalle Expediente–DocumentoAdjunto).
4. **Core Académico** (`coreacademico`): curso, seccion (+HorarioAula, cupos atómicos, cruces de aula/docente), plan (malla + prerrequisitos), docente.
5. **Matrícula**: adaptadores reales; reglas de periodo, estudiante, sección, cupos, cruces, duplicados, prerrequisitos; reserva de cupos concurrente; anulación.
6. **Finanzas** (`finanzas`): ConceptoPago, Cargo–DetalleCargo, cuotas sin error de redondeo, idempotencia por `(PROCESO_ORIGEN, ID_REFERENCIA)`, saldos, pagos, reporte; generación del cargo en la misma transacción de la matrícula.
7. **Pruebas**: unitarias, `@WebMvcTest`, Modulith, integración con Oracle real (incluye rollback y concurrencia), pruebas HTTP del backend levantado.
8. **Documentación**: arquitectura, matriz de requisitos, resultados, pendientes.

## 3. Estado de avance (2026-10-24)

| Etapa | Estado | Evidencia |
|---|---|---|
| A. Arquitectura unificada | Completa | `ModularityTests` 3/3, grafo acíclico |
| B. Base Universal | Completa | institucional, persona, alumno, expediente |
| C. Core Académico | Completa | curso, seccion, plan, docente |
| D. Matrícula | Completa (prerrequisitos con resultado APROBADO, V4) | 23 unitarias, 8 de integración |
| E. Finanzas | Completa | conceptos, cargos, cuotas, pagos, saldos |
| F. Preparación frontend | Completa | 84 operaciones en OpenAPI, CORS, `ErrorResponse` único |
| Pruebas | 83/83 automatizadas, 99/99 HTTP repetibles (×2) | `RESULTADOS_PRUEBAS.md` |

### Cambios aplicados en Oracle (esquema LAMB_APP)

1. Respaldo `expdp` previo (`infra/backups/lamb_app_pre_integracion_20261024_122636.dmp`, verificado).
2. `infra/oracle/V2__integracion_modulos.sql` aplicado: 16 tablas nuevas; columnas nuevas nullable en
   `MATRICULAS` (`TOTAL_CREDITOS`, `CLAVE_ACTIVA`, `FECHA_ANULACION`) y `DETALLE_MATRICULA`
   (`CURSO_ID`, `CREDITOS`); restricciones `UK_MATRICULA_ACTIVA` y `UK_DET_MATRICULA_SECCION`.
3. `infra/oracle/V3__datos_demo.sql` aplicado: datos ficticios (sede/facultad/escuela, 5 personas,
   3 alumnos 1001–1003, 6 cursos, malla vigente, periodo 2026-I cerrado con historial de Luis,
   7 secciones 1001–1007, 3 conceptos de pago).
4. Datos creados por las pruebas HTTP de demostración (persona 70000004, expediente EXP-2026-0001,
   matrículas de Ana y Luis con sus cargos y un pago).
5. Segundo respaldo `infra/backups/lamb_app_pre_v4_20261024_132510.dmp` (verificado) y
   `infra/oracle/V4__resultado_detalle_matricula.sql` aplicado: `DETALLE_MATRICULA.RESULTADO` y
   `FECHA_RESULTADO` (NULL en filas existentes) + 2 CHECK.
6. No se ejecutó ningún `DROP`, `TRUNCATE` ni `DELETE` sobre datos previos. Las pruebas de integración
   y las HTTP repetibles solo borran las filas que ellas mismas crean (verificado por conteo).

### Commits en la rama `integracion/backend-unificado`

- `de5a4a5` integración de módulos.
- `56243e4` pruebas y datos de demostración.
- Último commit: documentación final (ver `git log`).

### Auditoría técnica final (2026-10-24)

- Diff completo contra `main` revisado: la Matrícula original se conserva (ver `MODULOS_LAMB.md` §1).
- A1–A7 resueltos o documentados (`PENDIENTES.md` §A); 8 correcciones (`PENDIENTES.md` §B).
- Oracle y Docker auditados en solo lectura: integridad 0 incoherencias, respaldos legibles.
- Nuevos documentos: `CONTRATOS_REST.md`, `MODULOS_LAMB.md`, `docs/ROTACION_CREDENCIALES.md`.
- **Fusionado localmente en `main` (fast-forward a `c707f36`) con autorización del usuario. No publicado** (sin push).

### Fusión local en `main` (2026-10-24)

| Verificación | Resultado |
|---|---|
| Estado previo | Árbol limpio (sin cambios ni archivos sin seguimiento fuera de los ignorados), sin stash, `main` sin commits ausentes en la rama |
| Método | Fast-forward (`main` era ancestro de la rama): sin conflictos ni commit de fusión |
| Respaldo de referencia | Rama local `respaldo/main-antes-fusion` → `4e5beab` (estado anterior de `main`) |
| Archivos de `main` | 46 de 47 presentes; el único retirado es `matricula/matricula/exception/GlobalExceptionHandler.java`, cuyos 6 manejadores siguen activos vía `exception/GlobalExceptionHandler` (las excepciones de Matrícula heredan de las bases). Recuperable: `git show respaldo/main-antes-fusion:backend/src/main/java/pe/edu/upeu/lambacademic/matricula/matricula/exception/GlobalExceptionHandler.java` |
| Desde `main` | `mvnw clean package`: BUILD SUCCESS, 83/83 pruebas, `ModularityTests` 3/3 |
| Oracle / Docker | Contenedor `running`, 0 reinicios, mismo volumen; 19 tablas, 0 restricciones inválidas, 7 matrículas previas intactas, 0 cupos incoherentes |
| Respaldos | Ambos `.dmp` con el mismo SHA-256 en host y contenedor; `impdp sqlfile` legible |
| `origin/main` | Sin cambios (`4e5beab`); `main` local 4 commits por delante |

### Cierre de seguridad (2026-10-24)

Claves de `LAMB_APP`, `PDBADMIN`, `SYSTEM` y `SYS` rotadas (la del commit `4e5beab` ya no sirve);
Oracle solo en `127.0.0.1:1522` con imagen fijada; hook de secretos activo; insumos protegidos en
`D:\LP2`. Corregido el `cargo: null` de las consultas de matrícula. Detalle: `docs/ROTACION_CREDENCIALES.md`
§0 y `RESULTADOS_PRUEBAS.md` §0. Sin push y sin reescribir el historial.

### Unidad II: frontend de Matrícula y Keycloak (2026-10-24, rama `unidad2/frontend-matricula`)

Keycloak 26.8 con realm propio y PKCE; backend como Resource Server con matriz por rol y filtro por
dueño (`alumno_id`); `DELETE` de secciones vía `SeccionEnUsoPort`; SPA Angular 22 en `frontend/` con
portal, inscripción cabecera-detalle, consulta, detalle con cargo, horario, reportes y los CRUD de
Periodo (S7) y Sección (S8). Pruebas: backend 104/104, frontend 67/67, Keycloak 8/8, matriz 22/22,
E2E 101/101, huella de Oracle idéntica. Detalle: `docs/lp2-demo-matricula.md` y
`RESULTADOS_PRUEBAS.md` §U2. Sin push ni fusión con `main`.

### Cómo reanudar

1. `git switch integracion/backend-unificado`
2. Copiar `backend/.env.example` → `backend/.env` e `infra/.env.example` → `infra/.env` con las claves.
3. `cd backend && .\mvnw.cmd clean test` (debe dar 86/86 con Oracle levantado).
   Con el backend levantado: `python -I infra/pruebas/e2e_repetible.py` (101/101, autolimpieza).
4. Revisar `PENDIENTES.md` (decisiones A1′, A3 y A4).
