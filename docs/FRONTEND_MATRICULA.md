# Frontend de Matrícula: arquitectura y decisiones (Unidad II)

Angular 22.2 (zoneless, componentes standalone, signals, Reactive Forms, Router, HttpClient),
TypeScript estricto, Vitest. Angular Material 22 solo donde aporta lógica (diálogo de confirmación y
autocomplete, S8 3.10) e íconos Lucide (`@lucide/angular`). Código en `frontend/`.

## 1. Organización (S7)

```
src/app/
  core/                     infraestructura transversal (S7, S11)
    auth/                   pkce.ts, jwt.ts, auth-service.ts, auth-callback.ts, roles.ts
    guards/                 auth-guard.ts, rol-guard.ts
    interceptors/           auth-interceptor.ts (token, 401/403), trace-id-interceptor.ts (X-Trace-ID)
    services/               api-service.ts (URL base), aviso-service.ts, error-http.ts
    layout/                 layout (encabezado, menú lateral, router-outlet), menu.ts
    inicio/ sin-permiso/ no-encontrado/
  shared/
    componentes/            etiqueta-estado, confirmar-dialogo (MatDialog)
    horario/                horario-util.ts (cruces), horario-semanal (vista semanal)
    validadores.ts          entero, rangoFechas, sinRepetidos, alMenosUnaLinea
  features/
    matricula/
      periodo/              modelo, servicio, lista, formulario     -> CRUD independiente (S7)
      seccion/              modelo, servicio, lista, formulario     -> CRUD dependiente (S8)
      matricula/            portal, formulario cabecera-detalle, consulta, detalle, horario, reportes (S9)
    core-academico/         curso-service, docente-service  (solo consultas que Matrícula necesita)
    base-universal/         alumno-service                  (idem)
    finanzas/               cargo-service                   (idem)
```

Diferencia con el pedido inicial: el layout vive en `core/layout/` porque así lo hace el docente
(S7, rama `s07-creacion-arquitectura-spa`, y S11 3.9); no se creó una carpeta `layout/` aparte.

Los componentes no llaman a `HttpClient`: cada recurso tiene su servicio (S8 3.3). La lista y el
formulario de cada tabla son componentes separados (S12 Tabla 2, pregunta de S8).

## 2. Pantallas

| Pantalla | Ruta | Guardas | Datos reales |
|---|---|---|---|
| Inicio público | `/` | — | — |
| A. Portal de matrícula | `/matricula` | authGuard | periodos, secciones del periodo; estudiante: matrícula propia, cursos y cargo (`/finanzas/cargos/{id}`); coordinación: `/matriculas/reporte` por estado |
| B+C. Inscripción (oferta + cabecera-detalle) | `/matricula/inscripcion` | authGuard + rol ESTUDIANTE/COORDINADOR/ADMIN | periodos, secciones del periodo, alumnos (personal), prerrequisitos |
| E. Consulta paginada | `/matricula/consulta` | idem | matrículas paginadas, periodos, cursos, secciones, alumnos |
| Detalle con cargo y cuotas | `/matricula/consulta/:id` | idem | matrícula, periodo, secciones, cursos, cargo de Finanzas |
| D. Horario semanal | `/matricula/horario?matriculaId=` | idem | matrícula y secciones de su periodo |
| Reportes | `/matricula/reportes` | rol COORDINADOR/ADMIN | `/matriculas/reporte`, `/secciones/resumen`, `/finanzas/cargos/resumen` |
| Periodos (S7) | `/matricula/periodos[/nuevo|/:id/editar]` | lista con sesión; escritura ADMIN | `/periodos` |
| Secciones (S8) | `/matricula/secciones[/nueva|/:id/editar]` | lista con sesión; escritura COORDINADOR/ADMIN | `/secciones`, `/cursos`, `/periodos`, `/docentes` |

### Formulario transaccional (S9)

- Cabecera: alumno (el del token para ESTUDIANTE; lista desplegable para coordinación), periodo
  activo, fecha de hoy (debe caer dentro del periodo), cuotas del cargo.
- Detalle: `FormArray` de líneas `{cursoId, seccionId}`; se agregan desde la tabla de oferta o con
  “Agregar curso”; la sección depende del curso elegido (lista desplegable dependiente).
- En vivo (`toSignal(valueChanges)` + `computed`): créditos totales, cruces de horario, vista semanal
  de la selección con los bloques en conflicto marcados y separados.
- Validaciones visuales: al menos un curso, curso repetido, sección no abierta o sin vacantes,
  cruces, prerrequisito elegido en la misma matrícula; prerrequisitos informados por curso.
- Confirmación explícita (MatDialog con el resumen) y **una sola petición** `POST /api/v1/matriculas`.
  El cargo lo genera el backend en la misma transacción; la SPA nunca crea cargos.
- Sin doble envío (`enviando`); errores reales del backend mostrados tal cual (409 de
  prerrequisitos, cupos, cruces, matrícula duplicada; 404; 400 con el detalle por campo; 403).

## 3. Reutilización del proyecto de Cristian (`ENTREGA-CRISTIAN.zip`)

| Pieza de Cristian | Decisión | Motivo |
|---|---|---|
| `core/aviso.service.ts` + `layout/aviso.component.ts` | **Reutilizado** (adaptado a `core/services/aviso-service.ts` y a la bandeja del layout) | Compatible con signals y con la guía; avisos de éxito y error |
| `core/menu.ts` (menú declarativo con roles) | **Reutilizado como patrón** (`core/layout/menu.ts`) | Menú y rutas con las mismas reglas; se cambió a íconos Lucide y a los roles de Keycloak |
| Validador de arreglo de `plan-form` (curso repetido) | **Reutilizado como patrón** (`shared/validadores.ts`, `sinRepetidos`) | Validación del FormArray a nivel de arreglo |
| `core/auth.service.ts`, `jwt.util.ts`, `token.interceptor.ts`, `features/login` | **Descartado** | JWT propio con login en la SPA y token en `localStorage`: contrario a S10b/S11 |
| `error.interceptor.ts` | **Reescrito** dentro de `auth-interceptor.ts` | Cerraba sesión ante 401 sin refrescar; S11 exige un refresco y 403 sin reintento |
| `role.guard.ts` con `data.roles` | **Reescrito** como `rolGuard(...roles)` | Patrón de fábrica de S11 3.7 |
| Componentes con plantillas en línea y `*ngIf/*ngFor` | **No copiados** | Se usa el control de flujo `@if/@for` y plantillas separadas como el docente |
| Backend antiguo, `seguridad-jwt-private.pem` | **No usado** | El backend vigente es el integrado; la clave privada no se copió |

## 4. Adaptaciones respecto de las guías

| Guía | Adaptación | Por qué |
|---|---|---|
| S7 3.5 (ícono `favicon.ico`, sin paquetes de íconos) | Íconos Lucide y `favicon.svg` propio | La misión pidió Lucide para una interfaz profesional; se usan solo íconos con función (menú, acciones) |
| S9 `confirm()` | Diálogo de Angular Material | Mostrar el resumen completo de la matrícula |
| S11 `AuthService.refrescar` | Una sola promesa compartida | Varios 401 simultáneos (la consulta pide 3 recursos a la vez) hacían 3 refrescos |
| S11 `authInterceptor` | No adjunta el token a hosts distintos de `apiBaseUrl` | El token no debe salir hacia otro dominio |
| S11 `environment` | Keycloak en `48080`, realm `lamb-academic`, cliente `lamb-frontend` | Instancia propia, distinta de la de BomERP (38080) |
| S8 3.10 autocomplete | El valor de la opción es el texto `código · nombre`; el id solo se fija al seleccionar | Igual criterio: escribir no basta para elegir |
| Angular Material `mat.core()` | No se usa (es un mixin vacío en v22); el tema se define con `mat.theme` | Cambio de la versión 22 |
| `npm start` en PowerShell | `npm.cmd start` | La política de ejecución de scripts del equipo bloquea `npm.ps1` |

## 5. Identidad visual

Azul marino institucional (`#0f2a4a`), blanco y grises claros, acento turquesa (`#1c9c97`), tipografía
IBM Plex Sans/Mono, sombras mínimas, sin gradientes ni animaciones decorativas. Estados de carga,
error y vacío en cada pantalla; tablas con desplazamiento horizontal y menú lateral desplegable en
móvil. No se usan logotipos ni recursos de otras instituciones.

## 6. Comparación con la guía S7 (Creación y arquitectura de la SPA)

La guía S7 está en los insumos con un nombre equivocado (`COMPROBANTE-WJQ7Q29.pdf`). Se leyó completa
y se contrastó con lo implementado:

| Punto de la guía S7 | Lo que pide | En LAMB | Estado |
|---|---|---|---|
| 3.3 | `ng new … --routing --style=css --ssr=false` | Proyecto creado con Angular CLI 22.2.4, CSS y sin SSR | Cumple |
| 2.4, 3.5 | Layout (encabezado, sidebar, menú) en `core/layout`, ruta padre con `router-outlet`; `App` solo con `router-outlet` | `core/layout/layout.*`; `app.ts` solo con `<router-outlet />` | Cumple |
| 3.6 | Página de inicio real en `/`, sin redirección | `core/inicio/inicio.ts` público | Cumple |
| 2.5, 3.6–3.7 | Rutas hijas del layout con `loadComponent` (carga perezosa) | Todas las pantallas son hijas del layout y perezosas | Cumple |
| 2.6, 3.8 | Modelos como `interface`, no `class` | `*.model.ts` con `interface` y `type` | Cumple |
| 3.7 | Carpeta por recurso con archivos planos (`--flat`) | `features/matricula/periodo/periodo-list.ts`, `periodo-form.ts`, `periodo-service.ts`, `periodo.model.ts` | Cumple |
| 2.7, 3.9 | `environment.ts` + `ApiService` (URL base en un solo lugar) | `environments/environment.ts`, `core/services/api-service.ts` | Cumple |
| 3.9 | `provideZonelessChangeDetection()` y `provideHttpClient(withInterceptors([traceIdInterceptor]))` | Ambos en `app.config.ts` (el primero se agregó en esta revisión para seguir la guía; Angular 22 ya lo usaba por defecto) | Cumple |
| 3.9 | Interceptor `X-Trace-ID` con `crypto.randomUUID()` | `core/interceptors/trace-id-interceptor.ts` | Cumple |
| 2.7, 3.10 | Servicio por recurso con `resource`; los componentes no importan `HttpClient` | `PeriodoService`, `SeccionService`, `MatriculaService`… | Cumple |
| 2.7 (nota) | `HttpClient` + `subscribe()` en lugar de `httpResource()` | Se usa `HttpClient` + `subscribe()` | Cumple |
| 3.11–3.15 | Lista y formulario separados, mensajes de carga/error/vacío, confirmación al eliminar, editar carga los datos actuales | `PeriodoList` / `PeriodoForm`; eliminar con diálogo; probado contra el backend real | Cumple |
| 3.5 | Reusar `favicon.ico`, sin paquetes de íconos | Lucide + `favicon.svg` | Diferencia documentada (§4) |
| 4.4 | Criterios mínimos (estructura, layout con dos rutas, servicio HTTP, interceptor, CRUD real) | Ver `docs/lp2-demo-matricula.md` §3 | Cumple |

## 7. Mejora de la interfaz (rama `unidad2/mejora-frontend-matricula`)

Mejora visual y de usabilidad sin cambiar endpoints, reglas de negocio, roles ni la arquitectura
`core/shared/features`. Punto de recuperación: etiqueta local `respaldo/antes-mejora-frontend-matricula`.

| Área | Cambio | Datos que usa |
|---|---|---|
| Distribución general | Contenido fluido hasta 1600 px; columna principal + panel lateral fijo (`.distribucion-con-panel`) que pasa a una columna bajo 1200 px; enlace “Saltar al contenido”; menú con `aria-current`, fondo y cierre con Escape en móvil; columnas secundarias ocultas en pantallas medianas | — |
| Portal (estudiante) | Franja del periodo con disponibilidad real; matrícula vigente con cursos, sección, docente, horario y créditos; vista “Mi semana de clases”; panel con el cargo y su saldo real y la próxima cuota; “Iniciar inscripción” solo si no hay matrícula y el periodo lo permite; historial | `/periodos`, `/matriculas` (filtrado por el token), `/secciones`, `/cursos`, `/finanzas/cargos/{id}` |
| Portal (coordinación) | Indicadores: matrículas activas (INICIADA + CONFIRMADA), anuladas, inscripciones a cursos de las activas, secciones abiertas, vacantes disponibles; accesos de gestión; secciones abiertas sin vacantes | `/matriculas/reporte?estado=`, `/secciones` |
| Inscripción | Búsqueda de alumno por código, nombre o documento (solo personal); oferta en 5 columnas (vacantes “disponibles de capacidad” y ocupadas); panel con resumen, validaciones y lo que falta antes de registrar. Se conservan el `FormArray`, las validaciones, la confirmación y el envío único | sin cambios |
| Horario | Calendario semanal con colores por curso, día actual, líneas de media hora, carriles para cruces, leyenda y vista de agenda en móvil; tabla de clases; impresión. Estudiante: su matrícula vigente sin buscar (nunca una anulada). Coordinación: búsqueda por periodo y alumno, con mensajes si no hay matrícula o solo hay anuladas | `/matriculas`, `/matriculas/{id}`, `/secciones`, `/alumnos` |
| Consulta | Filtros con búsqueda de alumno, tamaño de página y “Limpiar filtros”; `aria-sort`; filas anuladas atenuadas y sin acción de horario; detalle expandido con docente y horario | sin cambios |
| Secciones y periodos | “Ocupados / capacidad” y “Disponibles” en lugar de “Vacantes”; eliminar deshabilitado con el motivo (tiene matriculados o periodo cerrado); confirmaciones que explican la consecuencia; avisos al editar un periodo cerrado o activar uno cuando ya hay otro activo. El `DELETE` de secciones no cambió | sin cambios |
| Reportes | Matrículas activas y anuladas por separado; cargos vigentes, pagos realizados, saldo pendiente, anulados (no cobrables) y total histórico; “No aplica” en lo pagado de un cargo anulado; ocupación con capacidad total, ocupados y disponibles | `/matriculas/reporte`, `/secciones/resumen`, `/finanzas/cargos/resumen` |

Componente reutilizable nuevo: `shared/componentes/estado-vacio.ts` (`app-estado-vacio`).
`HorarioSemanal` pasó a plantilla y estilos propios (`horario-semanal.html/.css`).

Cálculo de los indicadores financieros (`indicadoresCargos` en `reporte-matricula.ts`): se apoya en
las reglas de `CargoService` del backend (un cargo ANULADO queda con saldo 0 y no admite pagos). Lo
pagado es `montoTotal − saldoPendiente` solo de los cargos no anulados; lo anulado nunca se suma a la
deuda ni a los pagos. No se modificó ningún cálculo del backend.

## 8. Pulido para la presentación (rama `unidad2/pulido-matricula-presentacion`)

| Área | Cambio | Fuente de datos |
|---|---|---|
| Registros históricos | `consistencia-matricula.ts` revisa cada matrícula: alumno existente en Base Universal, secciones existentes en la oferta de su periodo, cursos identificados y créditos registrados. Portal y reportes cuentan solo las consistentes y muestran la conciliación con el total del servidor; consulta y detalle marcan las que requieren revisión. Nada se modifica ni se elimina | `/matriculas?periodoId` (paginado), `/alumnos`, `/secciones`, `/matriculas/reporte` |
| Escuela y plan del alumno | Nuevo `GET /api/v1/matriculas/contexto` (módulo Matrícula, aditivo): escuela y habilitación del alumno; el ESTUDIANTE solo el suyo. La inscripción consulta el plan vigente de esa escuela y los prerrequisitos con `idEscuela` (los mismos que valida el registro) | `/matriculas/contexto`, `/planes-estudio?idEscuela&estado=VIGENTE`, `/cursos/{id}/prerrequisitos?idEscuela` |
| Inscripción | Ciclo, tipo y prerrequisito de cada curso según el plan; orden y filtro por ciclo; cursos fuera del plan marcados; alumno no habilitado no puede registrar | idem |
| Estudiante | Menú propio (“Mis matrículas”, “Mi horario”), sin la gestión académica; portal con escuela, plan, ciclo y resultado de cada curso, y lo pagado según las cuotas | `/finanzas/cargos/{id}` (su cargo) |
| Roles | Periodos y Secciones pasan a ser pantallas de coordinación (ruta + menú); el cajero solo ve el portal | — |
| Login | Tema `lamb` de Keycloak con la identidad UPeU (ver `docs/SEGURIDAD_KEYCLOAK.md`) | — |

`PlanEstudioService` es de solo lectura: las pantallas de cursos y mallas son de Core Académico.
