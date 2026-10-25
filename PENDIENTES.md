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
| A4 | Credenciales en Git | **Resuelto** (D1, D2, D3, D6 aplicados; D4 no autorizado; D5 pospuesto) | Auditoría completa, procedimiento de rotación, ensayo de saneamiento y prevención en `docs/ROTACION_CREDENCIALES.md`. Única exposición en el historial: commit `4e5beab` (publicado en `origin/main`; el repo aparenta ser privado). Ese valor ya **no sirve**: las 4 cuentas se rotaron (rechaza `ORA-01017`) y el contenedor se recreó con las variables nuevas; solo persiste como verificador en los respaldos previos a la rotación. Hook `pre-commit` activo (`core.hooksPath`), `.gitignore` ampliado, Oracle solo en `127.0.0.1:1522`, insumos excluidos en `D:\LP2`. Evidencia: §0 del documento. |
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

## B′. Corrección posterior

| # | Problema | Causa | Corrección |
|---|---|---|---|
| B9 | `GET /api/v1/matriculas/{id}` y el listado devolvían `"cargo": null` aunque el cargo existiera (p. ej. matrícula 93 / cargo 7) | Solo `registrar` y `generarCargo` agregaban el cargo; `obtener`, `buscar`, `anular` y `registrarResultado` usaban el mapper, que ignora `cargo` (vive en Finanzas). No era un problema de consulta ni de datos previos | Consulta de solo lectura del cargo en Finanzas (individual y en lote para el listado, sin N+1). 3 pruebas unitarias de regresión (fallan sin la corrección) + integración Oracle + 2 casos HTTP. No se generaron cargos ni se tocaron pagos |

## C. Pendientes técnicos

| # | Pendiente | Prioridad |
|---|---|---|
| C1 | Revisión de cada módulo por su autor (`MODULOS_LAMB.md`) | Alta, antes de sustentar |
| C2 | Pedir a cada compañero que active el hook en su clon (`git config core.hooksPath .githooks`) y compartirles por canal privado las claves que necesiten | Media |
| C3 | CORS en navegador real (`fetch` desde `python -m http.server 5500`, S5 3.8) en la demo | Media |
| C4 | Objetos PL/SQL de BD2 de LP2_G1 (funciones, triggers, procedimientos): no aplicados porque sus scripts traen `DROP TABLE`; extraer solo los objetos y revisar que no dupliquen cálculos del backend | Media |
| C5 | ~~Seguridad JWT~~ → Keycloak integrado en U2 (`docs/SEGURIDAD_KEYCLOAK.md`) | Resuelto |
| C6 | Estado de matrícula como texto libre (contrato original); evaluar enumerado | Baja |
| C7 | Las pruebas de integración usan el esquema DEV (se autolimpian y verifican conteos) | Baja |
| C8 | Advertencia interna de springdoc (`JsonSchema type`, Jackson 3): no afecta la documentación generada | Baja |
| C9 | `ACTIVO NUMBER(1)` con convertidor: si BD2 migra a `BOOLEAN` de 23ai, retirar `BooleanNumeroConverter` | Baja |
| C10 | Escalamiento con dos instancias (S1 3.3) no probado | Baja |
| C11 | Adjuntos: se guarda ruta y metadatos, no el archivo | U2 |

## E. Pendientes de la Unidad II

| # | Pendiente | Prioridad |
|---|---|---|
| E1 | Evidencias con captura de reloj y usuario (S11 4.3) tomadas por el estudiante: login tecleado por una persona en Keycloak, expiración real (bajar *Access Token Lifespan* a 1 min) | Alta, antes de sustentar |
| E2 | Revisión por el autor de Core Académico del nuevo `DELETE /api/v1/secciones/{id}` y `SeccionEnUsoPort`: solicitud detallada en `docs/REVISION_DELETE_SECCIONES.md` | Alta |
| E3 | Cada compañero: `generar_secretos.py` + levantar Keycloak + `sembrar_usuarios.py` en su equipo (las claves no viajan por Git) | Media |
| E4 | ~~Guía S7~~: estaba en los insumos como `COMPROBANTE-WJQ7Q29.pdf`; ya se comparó (`docs/FRONTEND_MATRICULA.md` §6). Sigue faltando la plantilla oficial `lp2-demo.md`: contrastar `docs/lp2-demo-matricula.md` cuando esté disponible | Baja |
| E5 | Rol DOCENTE no implementado (no hay pantallas de docente en esta unidad) | Baja |
| E6 | Editar los horarios de una sección ya programada: el contrato del backend no lo permite (solo docente y vacantes) | Baja |
| E7 | Keycloak en `start-dev` sin TLS ni hostname: solo desarrollo (S10b 9) | Producción |
| E8 | Interfaces de Finanzas, Base Universal y Core Académico (fuera del alcance de Matrícula) | U3 |
| E9 | Pruebas de navegador automatizadas (Playwright) | Baja |

## D. Riesgos conocidos

- `infra/pruebas/e2e_demo.py` (histórico) crea datos fijos y no debe volver a ejecutarse.
- `V2__rollback.sql` y `V4__rollback.sql` eliminan columnas/tablas **con sus datos**: solo con autorización y respaldo.
