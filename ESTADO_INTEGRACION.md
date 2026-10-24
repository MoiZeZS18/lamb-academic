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

## 3. Estado actual

Ver la sección «Estado de avance» al final de este documento.
