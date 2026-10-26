# Resultados de pruebas — LAMB Academic Backend

Rama `integracion/backend-unificado` · Oracle AI Database 26ai Free (`lamb-academic-oracle`,
esquema `LAMB_APP`) · Java 21.0.12 · Spring Boot 4.0.8.

## U2-P. Pulido de Matrícula para la presentación (2026-10-26, rama `unidad2/pulido-matricula-presentacion`)

| Verificación | Resultado |
|---|---|
| Frontend `ng test` | 133/133 (25 archivos; 45 pruebas nuevas: consistencia de matrículas, reportes y portal con el caso real de 2026-II, escuela/plan/ciclo y prerrequisitos por escuela, periodo-list, auth-callback, acciones del detalle por rol, 16 rutas por rol) |
| Frontend `ng build` (producción) | Correcto, sin advertencias |
| Backend sin Oracle (`mvnw test` excluyendo `MatriculaOracleIntegrationTest`, `MatriculaRollbackOracleIntegrationTest` y `LambAcademicApplicationTests`) | 99/99 en 10 clases (incluye +4 pruebas del endpoint `/matriculas/contexto`). Las 3 clases excluidas usan el Oracle real y no se ejecutaron |
| `verificar_keycloak.py` | 8/8 con las claves de demostración (login PKCE y roles de las 5 cuentas) |
| `matriz_accesos.py` | 27/27 (5 casos nuevos de `/matriculas/contexto`) |
| `e2e_repetible.py` | No ejecutado: escribe en el Oracle real (aunque limpia sus datos), y esta fase exigía no escribir fuera de un entorno aislado |
| Navegador real | Coordinador: reportes (2 consistentes + 7 en revisión, conciliación 5 = 5), portal, consulta, inscripción con plan y ciclos. Estudiante: menú propio, portal con plan, ciclo, resultado y pagos; 3 rutas de gestión → “Acceso no permitido”. Admin: periodos, secciones, horario. Login UPeU en 1440, 768 y 375 px; clave equivocada y cierre de sesión |
| Huella de Oracle | Idéntica antes y después de todo el trabajo |

## U2-M. Mejora integral del frontend de Matrícula (2026-10-25, rama `unidad2/mejora-frontend-matricula`)

Cambios solo en la SPA (detalle en `docs/FRONTEND_MATRICULA.md` §7); backend, Keycloak y Oracle sin cambios.

| Verificación | Resultado |
|---|---|
| Frontend `ng test` | 86/86 (20 archivos; 19 pruebas nuevas: portal por rol, horario de estudiante y de coordinación, reportes e `indicadoresCargos`, secciones, búsqueda de alumno y pendientes de la inscripción, avisos de periodos, `EstadoVacio`) |
| Frontend `ng build` (producción) | Correcto, sin advertencias de presupuesto |
| Backend `mvnw test` | 104/104 |
| `verificar_keycloak.py` · `matriz_accesos.py` · `e2e_repetible.py` | 8/8 · 22/22 · 101/101 (limpieza: conteos de las 19 tablas iguales al inicio) |
| Navegador real (Keycloak + backend + Oracle) | Coordinador: portal, inscripción, consulta, detalle, horario, secciones (diálogo cancelado), reportes. Estudiante `ana.quispe`: portal con su matrícula y saldo real, horario sin búsqueda, 403 al abrir la matrícula de otro estudiante, `/matricula/reportes` → “Acceso no permitido”. Admin: avisos del formulario de periodos (sin guardar) y diálogo de eliminación (cancelado) |
| Anchos 1440 px y 375 px | Sin desplazamiento horizontal de la página (se corrigió un texto oculto que desbordaba las tablas); agenda por día y menú con fondo y cierre con Escape en móvil |
| Huella de Oracle | Idéntica antes y después de la verificación en el navegador y del E2E |

## U2-R. Revisión de idioma y de la guía S7 (2026-10-24)

Correcciones: mensajes visibles de la SPA sin anglicismos ni la palabra “backend”; mensajes 401/403
del backend con tildes y sin “claim”; Keycloak con el español como idioma predeterminado también en la
consola (realm `master`) y “Usuario o correo electrónico” en el login; `provideZonelessChangeDetection()`
explícito como en S7 3.9. Resultados después de las correcciones:

| Verificación | Resultado |
|---|---|
| Backend `mvnw clean package` | 104/104 |
| Frontend `ng test` / `ng build` | 67/67 / correcto |
| `verificar_keycloak.py` · `matriz_accesos.py` · `e2e_repetible.py` | 8/8 · 22/22 · 101/101 |
| Pantalla de login de Keycloak | `lang="es"`; todos los textos en español |
| 403 en la SPA | “No tienes acceso a la matrícula de otro estudiante” |
| Huella de Oracle | Idéntica a la del cierre de seguridad |

## U2. Unidad II: frontend de Matrícula y Keycloak (2026-10-24, rama `unidad2/frontend-matricula`)

| Verificación | Comando | Resultado |
|---|---|---|
| Backend completo (incluye matriz de seguridad, ABAC y DELETE de secciones) | `cd backend; .\mvnw.cmd clean package` | **104/104** (86 previas + 13 `SecurityConfigTest` + 1 `MatriculaServiceTest` + 4 `SeccionServiceTest`); `ModularityTests` 3/3 |
| Frontend (Vitest) | `cd frontend; npx ng test --watch=false` | **67/67** en 15 archivos |
| Compilación de producción | `npx ng build` | correcta; 325 kB iniciales (88 kB comprimidos) |
| Cliente de Keycloak | `python -I infra/pruebas/verificar_keycloak.py` | **8/8**: password grant `unauthorized_client`; sin `code_challenge` `invalid_request`; login PKCE de los 5 usuarios con rol y `alumno_id`; refresco |
| Matriz de accesos real | `python -I infra/pruebas/matriz_accesos.py` | **22/22** (401 sin token y con token alterado, 403 por rol y por dueño, 2xx con el rol correcto) |
| HTTP repetibles con token de Keycloak | `python -I infra/pruebas/e2e_repetible.py` | **101/101**, conteos de las 19 tablas iguales al inicio |
| Datos de Oracle | `huella_datos.sql` | huella **idéntica** a la del cierre de seguridad, después de todas las pruebas |

Pruebas en el navegador (panel del escritorio, backend y Keycloak reales). El formulario de login de
Keycloak lo completó `infra/pruebas/completar_login_navegador.py` para no escribir claves en ningún
registro; el intercambio del código con PKCE lo hizo la SPA.

| Caso | Resultado observado |
|---|---|
| Ruta protegida sin sesión | Redirige a Keycloak con `code_challenge_method=S256` y `state`; vuelve al destino pedido |
| Almacenamiento | `sessionStorage`: solo `lamb_sesion`; `localStorage` vacío |
| F5 con sesión | La sesión se mantiene |
| Inscripción (José) | Cruce IS101/IS104 marcado y envío bloqueado; 409 real de prerrequisito (IS201 requiere IS101); registro de IS101+IS102 → matrícula con cargo S/ 550 en 5 cuotas generado por el backend |
| 403 por dueño | `/matricula/consulta/93` como José → “No tienes acceso a la matricula de otro estudiante” |
| rolGuard | `/matricula/reportes` como estudiante → `/sin-permiso` |
| 401 → refresco | Token alterado: 3×401 → **1** refresco → 3×200, sin bucle |
| Logout | Sesión borrada; la ruta protegida vuelve a pedir login |
| Anular (coordinación) | Matrícula anulada, cupos liberados, cargo y cuotas anulados |
| CRUD Sección | Sin curso: mensaje sin petición; autocomplete; 409 de cruce de docente; alta IS202-B; 409 al eliminar IS101-A; baja IS202-B |
| CRUD Periodo (admin) | Alta 2099-I, edición, 409 al eliminar 2026-II, baja 2099-I |
| Limpieza | La matrícula de prueba (anulada) se retiró con `limpiar_matricula_prueba.py`, que rechaza matrículas no anuladas (probado con la 93) |

No ejecutado: espera real de 5 minutos para la expiración (se provocó el 401 alterando el token);
pruebas de navegador automatizadas (Playwright/Cypress no están en el proyecto); login tecleado en
el formulario de Keycloak por una persona.

## 0. Cierre de seguridad (2026-10-24, noche)

| Verificación | Resultado |
|---|---|
| Claves nuevas / antiguas (`verificar_credenciales.py`) | `LAMB_APP`, `PDBADMIN`, `SYSTEM`, `SYS`: clave vigente **CONECTA**; clave comprometida **RECHAZADA `ORA-01017`** (antes y después de recrear el contenedor) |
| Backend ↔ Oracle | `LambAcademicApplicationTests` 1/1 tras la rotación; arranque real en 7,8 s; `/actuator/health` `UP`, `db: UP` |
| Suite completa (`mvnw clean package`) | **86/86** (las 83 anteriores + 3 de regresión del cargo); `ModularityTests` 3/3 |
| Swagger / OpenAPI | `/swagger-ui.html` → 302 → `/swagger-ui/index.html` 200; 85 operaciones |
| Pruebas HTTP repetibles | **101/101** (99 + 2 de regresión); conteos de las 19 tablas iguales al inicio |
| Datos existentes | Huella (filas + hash de contenido de las 19 tablas, rol, cuota, 88 objetos) **idéntica** antes y después; cargos y pagos sin cambios |
| Contenedor | `running`, 0 reinicios, volumen `lamb-academic-dev_oracle-data`, puerto solo `127.0.0.1:1522` |
| Regresión `cargo: null` | Sin la corrección fallan `obtenerIncluyeCargoExistente` y `buscarIncluyeCargosEnLote`; con ella 26/26. En vivo: matrícula 93 → cargo 7 (700, 5 cuotas, PAGADO_PARCIAL), 94 → cargo 8; las 7 previas sin cargo |

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
