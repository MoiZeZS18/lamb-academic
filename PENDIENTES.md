# Pendientes y decisiones — LAMB Academic Backend

Actualizado tras la auditoría técnica final (2026-10-24). Estado: **Resuelto** · **Mitigado**
(resuelto en código, falta una acción humana) · **Pendiente** · **Decisión** (requiere al usuario).

## A. Resolución de A1–A7

| # | Tema | Estado | Qué se hizo / qué falta |
|---|---|---|---|
| A1 | Prerrequisitos: cursado ≠ aprobado | **Resuelto** (con una limitación documentada) | Sin crear un módulo de notas, cada detalle de matrícula registra su **resultado final** (`APROBADO`, `DESAPROBADO`, `RETIRADO`; V4) mediante `PATCH /api/v1/matriculas/{id}/detalles/{detalleId}/resultado`, solo con el periodo `CERRADO`. La política por defecto `lamb.matricula.prerrequisitos.politica=APROBADO` exige el resultado APROBADO en un periodo anterior. `CURSADO_TEMPORAL` (acepta cursos sin resultado, nunca DESAPROBADO/RETIRADO) queda como **política temporal identificada en el mensaje de error**, para usar solo durante la carga inicial de resultados; `NINGUNA` desactiva la regla. **Limitación:** no hay notas, actas ni promedios: el resultado se registra directamente; un módulo de evaluación (U2+) debería alimentarlo. |
| A1′ | Matrícula de Luis (demo) en IS201 | **Decisión** | Se registró con la política anterior («llevado»). Con la política vigente sería rechazada porque su IS101 de 2026-I no tiene resultado. No se modificó (datos de demo conservados). Para la demo en vivo puede registrarse `APROBADO` en ese detalle con el endpoint anterior (periodo 2026-I está CERRADO). |
| A2 | Siete matrículas anteriores | **Resuelto** (conservadas) | Intactas (ids 1, 25–29, 41: estudiante 1, periodo 4, `INICIADA`, `CLAVE_ACTIVA` NULL). Compatibilidad verificada: se consultan por la API, no ocupan cupos, no bloquean nuevas matrículas, no admiten cargo ni resultado (sin créditos ni curso). Siguen apuntando al estudiante 1 y a secciones 1, 2, 100, 101, que no existen: es su estado original y no se alteró. |
| A3 | Carpeta duplicada `lamb-academic/lamb-academic/` | **Decisión** (puede borrarse) | Auditada por completo: es un **clon Git propio** en `cbbb88a` (commit ya presente en el repo principal; sin commits, ramas ni stash propios; mismo `origin`). Sus `backend/` e `infra/` sin seguimiento coinciden con `4e5beab` salvo fines de línea, dos carpetas vacías (`static`, `templates`), `target/`, `HELP.md` y el `compose-dev.yml` original **con claves en texto plano**. Ningún contenedor, compose ni configuración la usa (el contenedor usa `D:\LP2\lamb-academic\infra`). No contiene nada que no esté en el repo principal. No se borró. |
| A4 | Credenciales en Git | **Preparado, pendiente de autorización** | Auditoría completa, procedimiento de rotación, ensayo de saneamiento y prevención en `docs/ROTACION_CREDENCIALES.md`. Única exposición en el historial: commit `4e5beab` (publicado en `origin/main`; el repo aparenta ser privado). El valor también persiste en variables del contenedor y en verificadores de los respaldos. Hook `pre-commit` y `.gitignore` ampliado preparados (hook sin activar). **Decisiones del usuario:** D1–D6 (sección 8 del documento). |
| A5 | Contratos REST que cambiaron | **Resuelto** | `CONTRATOS_REST.md`: cambios transversales y por módulo frente al código original de cada aporte, con impacto en el frontend. |
| A6 | `lp2-demo.md` | **Pendiente académico** (no bloquea la integración) | Producto de evaluación de S6; la plantilla del docente no está en el material recibido. Puede armarse con `ARQUITECTURA_LAMB.md`, `MATRIZ_REQUISITOS.md`, `RESULTADOS_PRUEBAS.md` y `MODULOS_LAMB.md` cuando se tenga la plantilla. |
| A7 | Descripción técnica por módulo | **Resuelto** | `MODULOS_LAMB.md`: origen, conservado/cambiado y por qué, reglas, API pública y pruebas de cada integrante. Falta que cada compañero lo revise. |

## B. Correcciones hechas en la auditoría final

| # | Problema encontrado | Corrección |
|---|---|---|
| B1 | `GET /api/v1/matriculas` paginaba **en memoria** (`HHH90003004`: `JOIN FETCH` de colección + `Pageable`, consulta original) | Sin `@EntityGraph` en esa consulta; `@BatchSize(50)` en `Matricula.detalles` |
| B2 | La respuesta paginada serializaba `PageImpl` tal cual (Spring Data advierte que no es un JSON estable) | `PaginacionConfig` con `VIA_DTO`: `{content, page{size, number, totalElements, totalPages}}` |
| B3 | Un cargo manual con `procesoOrigen=MATRICULA` (sobre todo sin referencia) duplicaba la deuda de la matrícula | Rechazado con 409; esos cargos solo los genera la matrícula |
| B4 | `GET .../estudiante/{id}/saldo` devolvía ceros para un estudiante inexistente | 404 |
| B5 | Una persona registrada como alumno podía cambiar a DOCENTE (alumno inconsistente) | 409 |
| B6 | Prerrequisitos aceptaban un curso solo cursado (A1) | Resultado por curso + política explícita |
| B7 | Las 46 pruebas HTTP no eran repetibles (creaban datos fijos) | `infra/pruebas/e2e_repetible.py`: datos aislados, autolimpieza, verificación de conteos |
| B8 | `HHH100123` (fetch size 10) | `hibernate.jdbc.fetch_size: 100` |

## C. Pendientes técnicos

| # | Pendiente | Prioridad |
|---|---|---|
| C1 | Revisión de cada módulo por su autor (`MODULOS_LAMB.md`) | Alta, antes de sustentar |
| C2 | Rotación de credenciales autorizada (A4) | Alta si el repo se comparte |
| C3 | CORS en navegador real (`fetch` desde `python -m http.server 5500`, S5 3.8) en la demo | Media |
| C4 | Objetos PL/SQL de BD2 de LP2_G1 (funciones, triggers, procedimientos): no aplicados porque sus scripts traen `DROP TABLE`; extraer solo los objetos y revisar que no dupliquen cálculos del backend | Media |
| C5 | Seguridad JWT (aporte LP2_L) | U2 (S10) |
| C6 | Estado de matrícula como texto libre (contrato original); evaluar enumerado | Baja |
| C7 | Las pruebas de integración usan el esquema DEV (se autolimpian y verifican conteos) | Baja |
| C8 | Advertencia interna de springdoc (`JsonSchema type`, Jackson 3): no afecta la documentación generada | Baja |
| C9 | `ACTIVO NUMBER(1)` con convertidor: si BD2 migra a `BOOLEAN` de 23ai, retirar `BooleanNumeroConverter` | Baja |
| C10 | Escalamiento con dos instancias (S1 3.3) no probado | Baja |
| C11 | Adjuntos: se guarda ruta y metadatos, no el archivo | U2 |

## D. Riesgos conocidos

- `infra/pruebas/e2e_demo.py` (histórico) crea datos fijos y no debe volver a ejecutarse.
- `V2__rollback.sql` y `V4__rollback.sql` eliminan columnas/tablas **con sus datos**: solo con autorización y respaldo.
