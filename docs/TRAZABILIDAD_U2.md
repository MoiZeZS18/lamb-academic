# Matriz de trazabilidad · Unidad II (S7–S12) · Matrícula

Cada fila indica la sesión y el ejemplo del docente que sirvió de base, dónde quedó en LAMB, el
endpoint real, la prueba y la evidencia. Rutas relativas a `frontend/src/app/` salvo que se indique.

| # | Requisito | Sesión / ejemplo del docente | Implementación LAMB | Endpoint | Prueba | Evidencia |
|---|---|---|---|---|---|---|
| 1 | Proyecto con `core/shared/features` | S7 2.5, 3.3–3.4; S12 Tabla 1 | `core/`, `shared/`, `features/` | — | `app.spec.ts`, build de producción | `ng build`: 88 kB iniciales comprimidos |
| 2 | Navegación en ruta padre con `router-outlet` | S7 2.4, 3.5–3.6 (`Layout`, `Inicio`) | `core/layout/layout.*`, `app.routes.ts` | — | `layout.spec.ts` | Navegador: menú y contenido |
| 3 | `X-Trace-ID` en cada petición | S7 3.9 (`traceIdInterceptor`) | `core/interceptors/trace-id-interceptor.ts` | todas | `SecurityConfigTest` (traza en 401) | Cabecera devuelta por el backend |
| 4 | URL base centralizada | S7 2.7, 3.9 (`ApiService`, `environment.ts`) | `core/services/api-service.ts` | — | `servicios.spec.ts` | — |
| 5 | CRUD independiente | S7 2.8, 3.10–3.15 (`CategoriaService/List/Form`) | `features/matricula/periodo/*` | `GET/POST/PUT/DELETE /api/v1/periodos` | `periodo-form.spec.ts`, `servicios.spec.ts` | Navegador ADMIN: crear/editar/eliminar 2099-I; 409 al eliminar 2026-II |
| 6 | Modelos que llega/que se envía | S8 3.2 `Producto/ProductoRequest` | `seccion.model.ts` (`Seccion` / `SeccionRequest` / `SeccionActualizacionRequest`) | `/api/v1/secciones` | `servicios.spec.ts` | Coinciden con el OpenAPI |
| 7 | Servicio de la tabla dependiente con filtro en servidor | S8 3.3, 3.5 | `seccion-service.ts` | `GET /api/v1/secciones?periodoId&cursoId&estado&conVacantes` | `servicios.spec.ts` | Filtros de la lista |
| 8 | Lista con información relacionada por nombre | S8 3.4 | `seccion-list.*` (curso anidado, docente) | idem | navegador | Lista de secciones |
| 9 | Lista desplegable con `[ngValue]` y preselección | S8 3.7 | `seccion-form.html` (periodo, docente) | `/periodos`, `/docentes` | `seccion-form.spec.ts` | Solo periodos no cerrados |
| 10 | Autocomplete (escribir no basta) | S8 3.10 | `seccion-form.ts` (`buscarCurso`, `onCursoSeleccionado`) | `/api/v1/cursos` | `seccion-form.spec.ts` | Navegador: filtro “softw” → IS202 |
| 11 | Validación de dependencia (sin opción, sin datos, 404) | S8 2.4, 3.8 | `seccion-form.ts` (`manejarErrorGuardado`) | `POST /api/v1/secciones` | `seccion-form.spec.ts` (sin curso, curso eliminado, sin cursos) | Navegador: “Selecciona un curso de la lista.” |
| 12 | Eliminar con confirmación y error controlado | S8 3.6 | `seccion-list.ts` + `SeccionEnUsoPort` (backend) | `DELETE /api/v1/secciones/{id}` (**nuevo**) | `SeccionServiceTest` (4), `SecurityConfigTest` | Navegador: 409 en IS101-A; IS202-B eliminada |
| 13 | FormArray de líneas con línea reutilizable | S9 3.4 | `matricula-form.ts` (`crearLinea`, `detalles`) | `POST /api/v1/matriculas` | `matricula-form.spec.ts` | Navegador |
| 14 | Cálculo en vivo con `toSignal` + `computed` | S9 3.5 | `totalCreditos`, `cruces`, `bloquesHorario` | — | `matricula-form.spec.ts` (créditos) | 2 cursos → 8 créditos |
| 15 | Agregar/quitar líneas, `track` por objeto | S9 3.6 | `agregarLinea`, `quitarLinea`, `elegirSeccion` | — | idem | Navegador |
| 16 | Validar al menos una línea | S9 2.4 | `alMenosUnaLinea` | — | `validadores.spec.ts`, `matricula-form.spec.ts` | — |
| 17 | Confirmación antes de guardar | S9 3.8 | `confirmar-dialogo.ts` | — | `matricula-form.spec.ts` (cancelar no envía) | Diálogo con resumen |
| 18 | Errores reales del backend con mensaje específico | S9 3.9 | `manejarErrorRegistro`, `core/services/error-http.ts` | 409/404/400/403 | `matricula-form.spec.ts` (409 prerrequisito, 409 cupos) | Navegador: 409 de prerrequisito IS201 |
| 19 | Una sola operación transaccional; cargo del backend | S4/S9 | `matricula-service.ts` `registrar` | `POST /api/v1/matriculas` | `servicios.spec.ts`, `MatriculaOracleIntegrationTest` | Matrícula con cargo CRG-… (S/ 550) |
| 20 | Consulta/reporte con filtros | S9 3.7, 3.10, 3.11 | `matricula-list.*`, `reporte-matricula.*` | `GET /api/v1/matriculas` (paginado), `/matriculas/reporte`, `/secciones/resumen`, `/finanzas/cargos/resumen` | `matricula-list.spec.ts` | Navegador coordinación |
| 21 | Acción “Anular” protegida | S9 3.12, S11 3.9 | `matricula-list`, `matricula-detalle` (solo coordinación) | `PATCH /api/v1/matriculas/{id}/anular` | `SecurityConfigTest`, `matriz_accesos.py` | Navegador: anulada, cupos liberados, cargo anulado |
| 22 | Horario semanal | Dominio LAMB (no está en las guías; patrón S9 de vistas de consulta) | `shared/horario/*`, `horario-matricula.*` | `/matriculas/{id}`, `/secciones?periodoId` | `horario.spec.ts` | Navegador: horario de la matrícula |
| 23 | Realm, roles, cliente | S10b 3–5 | `infra/keycloak/realm/lamb-academic-realm.json`, `sembrar_usuarios.py` | Keycloak | `verificar_keycloak.py` | 5 usuarios con su rol |
| 24 | Backend valida tokens de Keycloak | S10b 7.1–7.2 | `SecurityConfig`, `application-dev.yml` (`issuer-uri`) | todos | `SecurityConfigTest`, `matriz_accesos.py` | 401 sin token o alterado |
| 25 | Identidad desde el token, no del request | S10b 7.3 (`vendedorId = sub`) | `IdentidadToken` (`alumno_id`), controladores de matrícula y cargos | `/matriculas/**`, `/finanzas/cargos/**` | `SecurityConfigTest`, `MatriculaServiceTest` | 403 al cambiar el id en la URL |
| 26 | CORS como bean de Security | S11 3.1b | `CorsConfig` | preflight | `CorsConfigTest`, `SecurityConfigTest` | E2E casos 99-100 |
| 27 | Cliente sin Direct access grants, PKCE obligatorio | S11 3.1 | realm JSON | Keycloak | `verificar_keycloak.py` (casos 1-2) | `unauthorized_client`; `invalid_request` sin challenge |
| 28 | PKCE + state | S11 3.2–3.3 | `core/auth/pkce.ts`, `auth-service.ts` | Keycloak `/auth`, `/token` | `pkce.spec.ts` (vector RFC 7636), `auth-service.spec.ts` | URL con `code_challenge_method=S256` |
| 29 | Ruta de retorno | S11 3.4 | `core/auth/auth-callback.ts` | — | `auth-service.spec.ts` (state inválido) | Vuelta al destino pedido |
| 30 | Decodificar claims | S11 3.5 | `core/auth/jwt.ts` | — | `jwt.spec.ts` | Nombre y rol en el encabezado |
| 31 | Guard de autenticación | S11 3.6 | `core/guards/auth-guard.ts` | — | `guards.spec.ts` | URL directa sin sesión → Keycloak |
| 32 | Guard de rol | S11 3.7 | `core/guards/rol-guard.ts`, `app.routes.ts` | — | `guards.spec.ts` | `/matricula/reportes` como estudiante → `/sin-permiso` |
| 33 | Interceptor: token, 401 un refresco, 403 sin reintento | S11 3.8 | `core/interceptors/auth-interceptor.ts` | — | `auth-interceptor.spec.ts` (6 casos) | Navegador: 3×401 → 1 refresco → 3×200 |
| 34 | Menú por rol, usuario real, logout | S11 3.9 | `core/layout/*`, `menu.ts` | Keycloak `/logout` | `layout.spec.ts`, `auth-service.spec.ts` | Logout limpia la sesión |
| 35 | Token nunca en `localStorage` | S11 2.3 | `auth-service.ts` | — | `auth-service.spec.ts` | `localStorage` vacío en el navegador |
| 36 | Matriz de accesos | S11 3.10, S10b 8 | `docs/SEGURIDAD_KEYCLOAK.md` §5 | todos | `matriz_accesos.py` (22) | 22/22 |
| 37 | Producto y sustentación | S12 | `docs/lp2-demo-matricula.md`, `docs/GUION_SUSTENTACION_U2.md` | — | — | — |
