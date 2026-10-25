# LP2 · Producto de la Unidad II · Módulo de Matrícula (LAMB Academic)

> Equivalente a `lp2-demo.md` (S12) para el módulo de Matrícula. La plantilla oficial no estaba en
> los insumos recibidos; esta estructura sigue lo que S12 declara que contiene: organización
> funcional, evidencias exigidas, casos de prueba, integración esperada con Keycloak, rúbrica y
> trazabilidad. **Nota histórica:** S10 construyó un JWT propio como paso pedagógico; lo evaluado
> aquí es el estado real posterior a S10b: Keycloak + Authorization Code con PKCE.

## 1. Organización funcional

SPA Angular 22 (`frontend/`) organizada en `core` (autenticación, guards, interceptores, layout),
`shared` (componentes, validadores, horario) y `features/matricula` (periodo, sección, matrícula).
Detalle en `docs/FRONTEND_MATRICULA.md`. El backend es el monolito modular Spring Boot 4 existente
(Base Universal, Core Académico, Matrícula, Finanzas) sobre Oracle 23ai, protegido como Resource
Server de Keycloak (`docs/SEGURIDAD_KEYCLOAK.md`).

Navegación modular: `/` (público) → `/matricula` (portal) → inscripción, consulta, detalle, horario,
reportes, periodos y secciones; `auth/callback` fuera del layout; `/sin-permiso` para rol
insuficiente.

## 2. Producto por sesión

| Sesión | Producto en Matrícula |
|---|---|
| S7 | SPA con `core/shared/features`, layout con `router-outlet`, inicio real en `/`, `ApiService`, `X-Trace-ID`, CRUD independiente de **Periodo académico** (comparación con la guía en `docs/FRONTEND_MATRICULA.md` §6) |
| S8 | CRUD dependiente de **Sección** (depende de Curso y Periodo; Docente opcional), listas desplegables y autocomplete, validación de la dependencia en el formulario y frente al 404/409 del backend |
| S9 | **Inscripción cabecera-detalle** con FormArray, créditos y horario en vivo, confirmación, errores reales; **consulta** paginada con filtros y **reportes** |
| S10 | (histórico) JWT propio; reemplazado en S10b |
| S10b | Keycloak (realm propio `lamb-academic`), backend validando sus tokens, roles por endpoint y filtro por dueño con `alumno_id` |
| S11 | Login con Authorization Code + PKCE, token en memoria (espejo en `sessionStorage`), guards, interceptor con refresco único ante 401, menú por rol, logout real |

## 3. Evidencias (verificadas el 2026-10-24 contra el backend real y Oracle)

| Evidencia exigida | Cómo se demuestra | Resultado verificado |
|---|---|---|
| Navegación modular | Menú lateral por rol; rutas perezosas por pantalla | ESTUDIANTE: 6 enlaces sin Reportes; COORDINADOR: + Reportes; CAJERO: solo consulta (prueba `layout.spec.ts` y navegador) |
| CRUD independiente | Periodos como ADMIN: crear 2099-I, editar, eliminar | Creado, editado (formulario cargado), eliminado; eliminar 2026-II → “tiene matriculas registradas y no puede eliminarse” |
| CRUD dependiente | Secciones como COORDINADOR | Sin curso → “Selecciona un curso de la lista.” sin petición; autocomplete filtra; docente con cruce → 409 “El docente 5 ya dicta la seccion 1004 (IS104) el LUNES…”; creada IS202-B; eliminar IS101-A → “ya tiene matriculas registradas. Cancelala en su lugar”; IS202-B eliminada |
| Cabecera-detalle | Inscripción como `jose.apaza` | IS101+IS104 → cruce marcado y envío bloqueado; IS101+IS102+IS201 → 409 real “Para matricularse en IS201 debe haber aprobado antes IS101”; IS101+IS102 → matrícula registrada con cargo generado por el backend (S/ 550, 5 cuotas) |
| Consultas y reportes | Consulta paginada, detalle con cuotas, horario semanal, reportes por periodo | Datos de Oracle; reportes de matrícula por estado, ocupación por curso y cargos por estado |
| Login Keycloak | `/matricula/inscripcion` sin sesión | Redirección a `localhost:48080/.../auth` con `code_challenge_method=S256` y `state`; vuelta al destino pedido |
| Token en memoria | Inspección de almacenamiento | `sessionStorage`: solo `lamb_sesion`; `localStorage`: vacío |
| 401 con refresco | Token alterado en la sesión y recarga | 3 peticiones → 401; **1** POST de refresco a Keycloak; 3 reintentos → 200; sin bucle |
| 403 sin reintento | José abre `/matricula/consulta/93` (de Ana) | 403 “No tienes acceso a la matrícula de otro estudiante”; sin refresco |
| Acción protegida | “Anular” solo para COORDINADOR/ADMIN | No aparece para ESTUDIANTE; el backend devuelve 403 si se fuerza (matriz 22/22) |
| Integración entre módulos | Matrícula → Core Académico (secciones, cupos) → Finanzas (cargo) → Base Universal (alumno) | Registro, anulación (cupos liberados, cargo anulado) y consulta del cargo con cuotas |

## 4. Casos de prueba automatizados

| Suite | Casos | Resultado |
|---|---|---|
| Backend `mvnw clean package` (JUnit, Spring Modulith, Oracle) | 104 (86 previos + 13 matriz de seguridad + 5 de servicio) | 104/104 |
| Frontend `ng test` (Vitest) | 67 en 15 archivos | 67/67 |
| `infra/pruebas/verificar_keycloak.py` | 8 | 8/8 |
| `infra/pruebas/matriz_accesos.py` (tokens reales PKCE) | 22 | 22/22 |
| `infra/pruebas/e2e_repetible.py` (HTTP, con token de Keycloak) | 101 | 101/101, conteos iguales al inicio |
| Huella de datos de Oracle (19 tablas) | antes / después de todas las pruebas | idéntica |

## 5. Integración esperada con Keycloak

| Pieza | Valor |
|---|---|
| Emisor (`iss`) | `http://localhost:48080/realms/lamb-academic` |
| Cliente | `lamb-frontend` (público, Standard flow + PKCE S256, sin Direct access grants) |
| Claims usados por el backend | `realm_access.roles`, `alumno_id`, `sub`, `preferred_username` |
| Claims usados por la SPA | `realm_access.roles` (menú, guards), `alumno_id`, `name` |
| Endpoints protegidos | Matriz de `docs/SEGURIDAD_KEYCLOAK.md` §5 |

Fila explicada en vivo (sugerida): `GET /api/v1/matriculas/94` con el token de `ana.quispe` → 403 por
ABAC (`alumno_id` 1001 ≠ estudiante 1002); con el de `coordinador` → 200.

## 6. Rúbrica (referencia S12 §5)

S12 remite la rúbrica a `lp2-demo.md`, que no está en los insumos. Se aplican sus criterios mínimos
de aceptación (S12 §4), todos con evidencia en la sección 3:

| Criterio mínimo | Estado |
|---|---|
| La SPA arranca y conecta con el backend real | Cumplido |
| El CRUD dependiente valida la relación antes de enviar, con un caso inválido probado | Cumplido |
| El formulario cabecera-detalle calcula en vivo y maneja un error real del backend | Cumplido |
| Login real contra Keycloak (Authorization Code + PKCE), sin formulario propio | Cumplido |
| Token en memoria, nunca en `localStorage`; refresco ante 401 y denegación ante 403 verificados en vivo | Cumplido (401 provocado alterando el token; la espera de 5 min queda para la sustentación) |
| Menú y una acción protegida reflejan las reglas reales del backend | Cumplido |
| Cada integrante responde una pregunta de la Tabla 2 | Ver `docs/GUION_SUSTENTACION_U2.md` |

## 7. Trazabilidad

Matriz requisito → archivo → endpoint → prueba → evidencia: `docs/TRAZABILIDAD_U2.md`.
