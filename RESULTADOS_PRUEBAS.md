# Resultados de pruebas — LAMB Academic Backend

Fecha de ejecución: 2026-10-24 · Rama `integracion/backend-unificado` · Oracle AI Database 26ai Free
(`lamb-academic-oracle`, esquema `LAMB_APP`) · Java 21.0.12 · Spring Boot 4.0.8.

## 1. Resumen

| Tipo | Ejecutadas | Resultado |
|---|---|---|
| Compilación Maven (`mvnw clean package`) | 1 | BUILD SUCCESS |
| Suite completa `mvnw test` | **75 pruebas** | **75 OK, 0 fallas, 0 omitidas** |
| Arranque real del backend (`java -jar`) contra Oracle | 1 | Arranca en ~17 s, `health: UP`, `db: UP` |
| Pruebas HTTP de punta a punta sobre el servidor levantado | **46 casos** | **46 OK, 0 fallas** |
| Verificación negativa de Modulith (violación provocada) | 1 | Detectada y rechazada (luego revertida) |
| Verificación directa en Oracle (SQL*Plus) | 4 consultas | Datos coherentes (ver §4) |

## 2. Suite automatizada (`cd backend && .\mvnw.cmd test`)

| Clase | Tipo | Pruebas | Qué demuestra |
|---|---|---|---|
| `ModularityTests` | Spring Modulith | 3 | Límites entre módulos, módulos detectados, documentación generada |
| `LambAcademicApplicationTests` | Contexto + Oracle | 1 | Arranque con `ddl-auto: validate` sobre las 19 tablas reales |
| `MatriculaServiceTest` | Unitaria (Mockito) | 17 | Éxito con cargo; periodo inexistente/inactivo/fecha fuera; estudiante inexistente/no habilitado; matrícula duplicada; sección repetida, inexistente, de otro periodo, cerrada, sin cupo; curso repetido; cruce; prerrequisito cumplido/no cumplido; fallo en la reserva propaga la excepción |
| `PlanEstudioServiceTest` | Unitaria | 8 | Totales calculados; reglas 1–3; prerrequisito en ciclo anterior y del mismo plan; curso inexistente; código duplicado |
| `SeccionServiceTest` | Unitaria | 10 | Periodo inexistente/cerrado; horas inválidas y fuera de franja; cruce interno y de aula; grupo duplicado; límite < ocupados; reserva sin cupo; cancelar con matriculados |
| `CalculadoraCargoTest` | Unitaria | 10 | Monto por conceptos; error sin créditos; **suma exacta de cuotas** (6 casos, p. ej. 1150/3 = 383.33+383.33+383.34) |
| `CargoServiceTest` | Unitaria | 6 | Cargo 150 + 50×20 = 1150; idempotencia; pago aplicado en orden; pago > saldo; anular con pagos; anulación deja saldo 0 |
| `MatriculaControllerTest` | `@WebMvcTest` | 5 | 201 + `X-Trace-ID`; 400 sin llegar al service; 409; 404; JSON mal formado |
| `PersonaControllerTest` | `@WebMvcTest` | 5 | 201; 400 con errores por campo; 409; 404; enum inválido |
| `CorsConfigTest` | `@WebMvcTest` | 2 | Dos orígenes por propiedad (con espacio); origen no permitido → 403 |
| `MatriculaOracleIntegrationTest` | Integración Oracle real | 6 | Registro completo + cupos + cargo con cuotas exactas; duplicada; sección llena sin reservas parciales; **dos hilos por el último cupo: exactamente 1 éxito**; anulación libera cupos y anula cargo; prerrequisito de malla real |
| `MatriculaRollbackOracleIntegrationTest` | Integración Oracle real | 2 | **Rollback provocado**: Finanzas falla después del INSERT de cabecera/detalles y del UPDATE de cupos → nada queda en Oracle; control sin fallo sí confirma |

Las pruebas de integración crean datos con códigos únicos y los eliminan por id al terminar
(verificado: los conteos de tablas quedaron iguales antes y después).

## 3. Pruebas HTTP de punta a punta (servidor real)

Script: `infra/pruebas/e2e_demo.py` (Python estándar, sin dependencias). Resultado completo:
`infra/pruebas/e2e_resultado_20261024.md`. **Crea datos de demostración** (persona 70000004,
expediente EXP-2026-0001, matrículas de Ana y Luis, un pago): no es repetible sin limpiar esos datos.

| # | Caso | Esperado | Obtenido | Evidencia |
|---|---|---|---|---|
| 1 | `GET /actuator/health` | 200 | 200 | status=UP db=UP |
| 2 | `GET /v3/api-docs` | 200 | 200 | 53 rutas (84 operaciones) |
| 4 | `GET /actuator/modulith` | 200 | 200 | baseuniversal, compartido, coreacademico, exception, filter, finanzas, matricula |
| 6–9 | Persona: crear / DNI inválido / duplicado / inexistente | 201/400/409/404 | igual | errores por campo |
| 10 | Filtro combinado de personas | 200 | 200 | 1 resultado |
| 11–12 | Alumno: registrar / docente como alumno | 201/409 | igual | — |
| 13–15 | Expediente cabecera-detalle / archivo inconsistente / verificación de rollback | 201/409/200 | igual | EXP-2026-0002 no existe |
| 18–19 | Malla vigente / prerrequisitos de IS201 | 200 | 200 | 21 créditos; IS101 |
| 20–21 | Sección con aula ocupada / docente ocupado | 409 | 409 | — |
| 23 | Carga docente | 200 | 200 | 3 secciones, 9 h |
| 24 | Matrícula de Ana (IS101, IS102, IS103) | 201 | 201 | 11 créditos, cargo 700.00, `X-Trace-ID=demo-matricula-ana` |
| 25–27 | Duplicada / cruce IS101–IS104 / prerrequisito no cumplido | 409 | 409 | — |
| 28 | Luis (llevó IS101 en 2026-I) en IS201 + IS103 | 201 | 201 | 500.00 en 3 cuotas |
| 29–30 | IS103 llena → Rosa (IS102 + IS103) / verificación de rollback | 409/200 | igual | IS102 no quedó reservada |
| 31–33 | Fecha fuera del periodo / sin detalles / estudiante inexistente | 409/400/404 | igual | — |
| 34–36 | Búsqueda paginada / reporte / ocupación | 200 | 200 | 9 matrículas, 14 detalles en el periodo 4 |
| 37–40 | Cuotas / pago parcial / pago excesivo / saldo | 200/201/409/200 | igual | 5×140; saldo 500 |
| 41–43 | Anular con pagos / cargo idempotente / cargo de matrícula legada | 409/200/409 | igual | mismo cargo |
| 45–46 | CORS desde 4200 / desde 5500 | 200/403 | igual | `Access-Control-Allow-Origin` solo para 4200 |

## 4. Verificación directa en Oracle

- Cupos: `SECCIONES.CUPOS_OCUPADOS` coincide con las matrículas activas de cada sección (1001:1, 1002:1, 1003:2/2, 1005:1, 1007:1).
- Cargos: `FIN_CARGOS` 7 (700.00, saldo 500.00, PAGADO_PARCIAL) y 8 (500.00, EMITIDO); la suma de cuotas de cada cargo es igual a su monto.
- Trazabilidad: `backend/logs/lamb-academic.log` contiene las líneas `[demo-matricula-ana]` de validación y reserva de cupos.
- Datos previos: las 7 matrículas originales (ids 1, 25–29, 41) siguen en estado `INICIADA` y sin cambios.

## 5. Lo que NO se pudo ejecutar o verificar

| Elemento | Motivo |
|---|---|
| CORS desde un navegador real (S5 3.8) | Se verificó con preflight HTTP (encabezados reales del servidor) y `@WebMvcTest`; la prueba con `fetch` en un navegador queda para la demostración en vivo. |
| Evidencias con capturas (reloj y usuario visibles) | Son evidencias individuales de cada estudiante (S1 4.3). |
| Frontend | Fuera de alcance (Unidad II). |
| Escalamiento con dos instancias (S1 3.3) | No se probó en esta sesión; la configuración lo permite (`--server.port=8081`). |
| Restauración del respaldo Data Pump | Se verificó que el dump es legible (`impdp … sqlfile=`), pero no se importó para no sobrescribir datos. |
