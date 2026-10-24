# Resultados de pruebas — LAMB Academic Backend

Rama `integracion/backend-unificado` · Oracle AI Database 26ai Free (`lamb-academic-oracle`,
esquema `LAMB_APP`) · Java 21.0.12 · Spring Boot 4.0.8.

## 1. Resumen de la auditoría final (2026-10-24, tarde)

| Verificación | Ejecución | Resultado |
|---|---|---|
| Compilación y suite completa (`mvnw clean package`, incluye pruebas) | Re-ejecutada tras las correcciones | **83 pruebas: 83 OK, 0 fallas** (las 75 anteriores + 8 nuevas) |
| Spring Modulith (`ModularityTests`) | Incluida en la suite | 3/3 OK; grafo acíclico |
| Conexión real a Oracle y validación de esquema (`ddl-auto: validate`, 19 tablas + V4) | `LambAcademicApplicationTests` y arranque `java -jar` | OK; `health: UP`, `db: UP`; 85 operaciones en OpenAPI; sin advertencias `HHH90003004`, `PageImpl` ni `HHH100123` |
| Pruebas HTTP repetibles (`infra/pruebas/e2e_repetible.py`) | **3 ejecuciones** (sufijos 09573, 26155 y 57258; la tercera tras la suite final) | **99/99 en las tres**; conteos de las 19 tablas idénticos al inicio en todas |
| 46 pruebas HTTP originales (`infra/pruebas/e2e_demo.py`) | **No re-ejecutadas a propósito** | No son repetibles (crean datos fijos: la segunda vez devuelven 409 donde esperan 201). Su cobertura está incluida en los 99 casos repetibles. Resultado histórico: 46/46 (`infra/pruebas/e2e_resultado_20261024.md`) |
| Auditoría de integridad de Oracle (solo lectura) | SQL*Plus | 0 incoherencias (ver §4) |
| Respaldos Data Pump | `sha256sum` + `impdp … sqlfile=` | Legibles e idénticos en host y contenedor |

## 2. Suite automatizada (`cd backend && .\mvnw.cmd clean test`)

| Clase | Tipo | Pruebas | Qué demuestra |
|---|---|---|---|
| `ModularityTests` | Spring Modulith | 3 | Límites entre módulos; módulos detectados; documentación generada |
| `LambAcademicApplicationTests` | Contexto + Oracle | 1 | Arranque con `validate` sobre el esquema real (incluye V4) |
| `MatriculaServiceTest` | Unitaria | 23 | Éxito con cargo; periodo inexistente/inactivo/fecha fuera; estudiante inexistente/no habilitado; duplicada; sección repetida, inexistente, de otro periodo, cerrada, sin cupo; curso repetido; cruce; **política APROBADO** (rechaza/acepta), **CURSADO_TEMPORAL** (identificada en el mensaje), **NINGUNA**; **resultado**: periodo abierto, detalle legado, registro correcto, detalle ajeno; fallo de reserva propaga excepción |
| `PlanEstudioServiceTest` | Unitaria | 8 | Totales; reglas 1–3; prerrequisito en ciclo anterior y del mismo plan; curso inexistente; código duplicado |
| `SeccionServiceTest` | Unitaria | 10 | Periodo inexistente/cerrado; horas inválidas/fuera de franja; cruces internos y de aula; grupo duplicado; límite < ocupados; reserva sin cupo; cancelar con matriculados |
| `CalculadoraCargoTest` | Unitaria | 10 | Monto por conceptos; error sin créditos; suma exacta de cuotas (6 casos) |
| `CargoServiceTest` | Unitaria | 8 | Cargo 1150; idempotencia; pago en orden; pago > saldo; anulación con/sin pagos; **cargo manual MATRICULA → 409**; **saldo de estudiante inexistente → 404** |
| `MatriculaControllerTest` | `@WebMvcTest` | 5 | 201 + `X-Trace-ID`; 400 sin llegar al service; 409; 404; JSON mal formado |
| `PersonaControllerTest` | `@WebMvcTest` | 5 | 201; 400 con errores por campo; 409; 404; enumerado inválido |
| `CorsConfigTest` | `@WebMvcTest` | 2 | Orígenes por propiedad; origen no permitido → 403 |
| `MatriculaOracleIntegrationTest` | Integración Oracle | 6 | Registro completo con cupos y cuotas exactas; duplicada; sección llena sin reservas parciales; **2 hilos por el último cupo: 1 éxito**; anulación; **prerrequisitos: sin curso previo se rechaza, resultado en periodo abierto se rechaza, APROBADO habilita, DESAPROBADO no** |
| `MatriculaRollbackOracleIntegrationTest` | Integración Oracle | 2 | **Rollback provocado** después de escribir cabecera, detalles y cupos; control sin fallo |

Las pruebas de integración crean datos con códigos únicos y borran solo esos ids al terminar.

## 3. Pruebas HTTP repetibles sobre el servidor real (99 casos)

Script: `infra/pruebas/e2e_repetible.py` · Resultado completo: `infra/pruebas/e2e_repetible_resultado_20261024.md`.

| Grupo | Casos | Cubre |
|---|---|---|
| Plataforma | 5 | `health` con db UP, OpenAPI, `/hello`, `actuator/modulith` (4 módulos), `X-Trace-ID` propagado |
| Base Universal | 26 | Sede → Facultad → Escuela (relaciones), sede con facultades activas (409), árbol; 6 personas y 5 alumnos; DNI inválido (400), duplicado (409), inexistente (404), alumno que pasa a DOCENTE (409), docente como alumno (409); filtro combinado + orden; expediente cabecera-detalle, rollback (409) y su verificación; flujo de estado |
| Core Académico | 19 | 6 cursos; malla con curso repetido (rollback 409) y malla válida con prerrequisito (14 créditos); prerrequisitos; distribución por ciclo; 6 secciones; aula ocupada (409); docente ocupado (409); carga docente |
| Matrícula | 24 | Periodo pasado propio; fechas invertidas (409); matrículas en el periodo pasado; resultado con periodo abierto (409); cierre; APROBADO/DESAPROBADO; resultado inválido (400); **Base → Core → Matrícula → Finanzas** (7 créditos, cargo en 3 cuotas); reserva de cupos; duplicada; prerrequisito DESAPROBADO y nunca llevado (409); cruce (409); sección llena sin reservas parciales; inexistente (404); sin detalles (400); fecha fuera (409); **concurrencia HTTP: 201 + 409, cupo 1/1**; anulación que libera cupo; `numeroCuotas=0` (400) |
| Finanzas | 10 | Cuotas = total; pago parcial; pago excesivo (409); saldo; saldo inexistente (404); cargo manual MATRICULA (409); cargo TRÁMITE por concepto; idempotencia; anular con pagos (409); cargo de la matrícula anulada en estado ANULADO |
| Compatibilidad | 2 | Matrícula previa 1 legible; su cargo no calculable (409) |
| Consultas y reportes | 11 | Paginación con contrato `VIA_DTO`, reporte, resumen; secciones por docente con vacantes; ocupación; resumen financiero; cargos filtrados; resúmenes de personas, alumnos, mallas y expedientes |
| CORS | 2 | 4200 permitido (ACAO); 5500 → 403 |

## 4. Auditoría de Oracle (solo lectura)

| Comprobación | Resultado |
|---|---|
| Migraciones aplicadas a la base del proyecto (`jdbc:oracle:thin:@localhost:1522/FREEPDB1`, `LAMB_APP`) | V2 (16 tablas + columnas), V3 (datos demo), V4 (`RESULTADO`, `FECHA_RESULTADO` + 2 CHECK) |
| Restricciones | 163 CHECK (161 + 2 de V4), 19 PK, 17 FK, 18 UNIQUE: todas `ENABLED VALIDATED`; índice funcional `UX_FIN_CARGOS_ORIGEN` válido |
| 7 matrículas y 9 detalles previos | Sin cambios (mismos valores; columnas nuevas en NULL) |
| Referencias sin respaldo | Solo las previas: 9 detalles → secciones 1, 2, 100, 101; 7 matrículas → estudiante 1 (nunca existieron en este esquema). 0 en el resto |
| Cupos | `CUPOS_OCUPADOS` = matrículas activas por sección: 0 diferencias |
| Finanzas | Cuotas = monto total: 0 diferencias; saldo = total − pagos: 0 diferencias; matrículas activas sin cargo: 0 |
| Contenedor | `running`, 0 reinicios, volumen `lamb-academic-dev_oracle-data`, hash de Compose sin cambios, base `OPEN/ACTIVE`, 3 % de disco |
| Respaldos | `lamb_app_pre_integracion_20261024_122636.dmp` (SHA-256 `9bbc5f6d…4864fe`) y `lamb_app_pre_v4_20261024_132510.dmp` (`6b099e56…14dc6`, 19 tablas): idénticos en host y contenedor, legibles con `impdp sqlfile` |

## 5. Lo que no se ejecutó

| Elemento | Motivo |
|---|---|
| Restaurar un respaldo | Sobrescribiría datos; solo se verificó su legibilidad |
| CORS desde un navegador real | Verificado con encabezados reales del servidor y `@WebMvcTest`; queda para la demo |
| Dos instancias simultáneas (S1 3.3) | No probado |
| Re-ejecutar las 46 pruebas originales | No repetibles por diseño; reemplazadas por las 99 repetibles |
