# Seguridad de LAMB Academic con Keycloak (S10b + S11)

> Estado: implementado y verificado el 2026-10-24 en la rama `unidad2/frontend-matricula`.
> Ninguna clave aparece en este documento ni en el repositorio.

## 1. Arquitectura

```
Navegador (SPA Angular, localhost:4200)
   │ 1. /auth con code_challenge (S256) + state ──────────────► Keycloak 26.8 (127.0.0.1:48080)
   │ 2. el usuario escribe su clave EN Keycloak (nunca en la SPA)   realm lamb-academic
   │ 3. /auth/callback?code&state  ◄──────────────────────────     Postgres propio (127.0.0.1:45432)
   │ 4. POST /token (code + code_verifier) ───────────────────►
   │    access_token (5 min) + refresh_token + id_token
   │ 5. Authorization: Bearer <access_token> ──► Spring Boot (localhost:8080), Resource Server
   │                                               valida firma (JWKS del realm) + iss (issuer-uri)
   │                                               rol (realm_access.roles) + dueño (alumno_id)
   │                                               └──► Oracle 23ai (127.0.0.1:1522) sin cambios
```

Keycloak vive en contenedores aparte (`infra/compose-dev.yml`: servicios `keycloak` y
`keycloak-db`) y no comparte nada con Oracle. El backend solo lo conoce por
`spring.security.oauth2.resourceserver.jwt.issuer-uri` (S10b 7.1).

## 2. Realm `lamb-academic` (versionado en `infra/keycloak/realm/lamb-academic-realm.json`)

| Elemento | Configuración | Por qué |
|---|---|---|
| Roles de realm | `ESTUDIANTE`, `COORDINADOR`, `ADMIN`, `CAJERO` | Roles del dominio universitario (no los de BomERP) |
| Cliente | `lamb-frontend`, público, *Standard flow* | Una SPA no puede guardar un secreto (S11 2.2) |
| Direct access grants | **Desactivado** | RFC 9700: no para clientes públicos (S11 3.1) |
| PKCE | `pkce.code.challenge.method = S256` (obligatorio) | Keycloak rechaza un login sin `code_challenge` |
| Redirect / logout / web origins | `http://localhost:4200/*`, `http://localhost:4200` | Solo la SPA local |
| Mapper `alumno_id` | atributo de usuario `alumnoId` → claim `alumno_id` (long) en access e id token | Vincula al usuario con `ALUMNOS.ID` (patrón DIST S07 citado en S10b 1) |
| Perfil de usuario | `alumnoId` editable **solo por el administrador** | Un estudiante no puede cambiar a qué alumno representa |
| Vida del access token | 300 s (valor por defecto, S10b 6) | Ventana corta; la SPA refresca ante 401 |
| Tema de login | `lamb` (hereda `keycloak.v2`, solo colores, marca y `messages_es.properties`) | Identidad LAMB sin copiar recursos institucionales |
| Idioma | `internationalizationEnabled`, `supportedLocales = [es]`, `defaultLocale = es`; la consola de administración (realm `master`) también en español (`sembrar_usuarios.py`, S10b 3.0) | Todo el sistema se presenta en español; “Usuario o email” se reemplazó por “Usuario o correo electrónico” |
| Fuerza bruta | activada (5 intentos) | Protección básica del login |

`--import-realm` solo importa el realm si aún no existe: los cambios hechos después en la consola
no se pisan al reiniciar.

## 3. Usuarios de prueba (`infra/keycloak/sembrar_usuarios.py`)

| Usuario | Rol | `alumnoId` (ALUMNOS.ID real de Oracle) |
|---|---|---|
| `ana.quispe` | ESTUDIANTE | 1001 (tiene la matrícula 93 y el cargo 7) |
| `jose.apaza` | ESTUDIANTE | 1033 (sin matrícula en 2026-II: sirve para la demo de inscripción) |
| `coordinador` | COORDINADOR | — |
| `admin.lamb` | ADMIN | — |
| `cajero` | CAJERO | — |

Las claves se generan con `infra/keycloak/generar_secretos.py` (módulo `secrets`) y viven solo en
`infra/secretos/keycloak-usuarios.env` (ignorado por Git, acceso solo del usuario de Windows). La
clave de administración de Keycloak y la de su Postgres están en `infra/.env`.

## 4. Relación usuario autenticado ↔ Alumno

- El estudiante **no** elige ni envía su identidad: el backend lee `alumno_id` del token ya validado
  (`compartido/seguridad/IdentidadToken`), igual que `vendedorId = sub` en S10b 7.3.
- `MatriculaRegistroRequest` conserva el campo `estudianteId` (contrato existente, lo usa la
  coordinación para matricular a cualquier alumno). Si quien llama es ESTUDIANTE y ese campo no
  coincide con su `alumno_id`, la respuesta es **403** y no se toca la base.
- Cambiar el id en la URL (`/matricula/consulta/93` siendo José) responde **403** “No tienes acceso
  a la matrícula de otro estudiante” (403 y no 404: existe, pero no es suya; S10b 7.3).
- Un ESTUDIANTE sin `alumnoId` asignado recibe 403 explicado (“Tu cuenta no está vinculada a ningún
  alumno. Solicita a la administración que registre tu código de alumno”).
- `GET /api/v1/auth/perfil` devuelve lo que el backend entiende del token (sub, usuario, roles del
  dominio, alumnoId), sin consultar ninguna base (S10 4.1).

## 5. Matriz real de roles y endpoints (`SecurityConfig`)

Leyenda: ✔ permitido · ✔* permitido solo sobre sus propios datos (ABAC) · ✘ 403 · Sin token: 401.

| Endpoint | ESTUDIANTE | COORDINADOR | ADMIN | CAJERO |
|---|---|---|---|---|
| `GET /actuator/health`, `/swagger-ui*`, `/v3/api-docs*`, `/api/v1/hello` | público | público | público | público |
| `GET /api/v1/auth/perfil` | ✔ | ✔ | ✔ | ✔ |
| `GET /api/v1/periodos/**` | ✔ | ✔ | ✔ | ✔ |
| `POST/PUT/DELETE /api/v1/periodos/**` | ✘ | ✘ | ✔ | ✘ |
| `POST /api/v1/matriculas` | ✔* | ✔ | ✔ | ✘ |
| `GET /api/v1/matriculas`, `/{id}` | ✔* | ✔ | ✔ | ✘ |
| `GET /api/v1/matriculas/resumen`, `/reporte` | ✘ | ✔ | ✔ | ✘ |
| `PATCH /matriculas/{id}/anular`, `POST /{id}/cargo`, `PATCH .../resultado` | ✘ | ✔ | ✔ | ✘ |
| `GET /api/v1/secciones/**`, `/cursos/**`, `/docentes`, `/planes-estudio/**` | ✔ | ✔ | ✔ | ✔ |
| `GET /api/v1/secciones/resumen`, `/docentes/{id}/carga` | ✘ | ✔ | ✔ | ✘ |
| `POST/PUT/PATCH/DELETE /api/v1/secciones/**` | ✘ | ✔ | ✔ | ✘ |
| escritura de `/cursos/**`, `/planes-estudio/**` | ✘ | ✘ | ✔ | ✘ |
| `GET /sedes`, `/facultades`, `/escuelas`, `/institucional/**` | ✔ | ✔ | ✔ | ✔ |
| `GET /api/v1/personas/**`, `/alumnos/**` (datos personales) | ✘ | ✔ | ✔ | ✔ |
| escritura de sedes, facultades, escuelas, personas, alumnos | ✘ | ✘ | ✔ | ✘ |
| `/api/v1/expedientes/**` | ✘ | ✔ | ✔ | ✘ |
| `GET /api/v1/finanzas/conceptos/**` | ✔ | ✔ | ✔ | ✔ |
| escritura de conceptos | ✘ | ✘ | ✔ | ✘ |
| `GET /finanzas/cargos` (listado), `/cargos/resumen` | ✘ | ✔ | ✔ | ✔ |
| `GET /finanzas/cargos/{id}`, `/{id}/pagos`, `/estudiante/{id}`, `/estudiante/{id}/saldo` | ✔* | ✔ | ✔ | ✔ |
| `POST /finanzas/cargos`, `PATCH /{id}/anular`, `POST /{id}/pagos` | ✘ | ✘ | ✔ | ✔ |
| `/actuator/**` (resto: métricas, modulith) | ✘ | ✘ | ✔ | ✘ |
| cualquier otra ruta | ✔ | ✔ | ✔ | ✔ |

Respuestas de error con el cuerpo común del backend (`timestamp, status, error, message, path,
traceId`): **401** conserva además la cabecera estándar `WWW-Authenticate: Bearer ...` (RFC 6750);
**401** dice “Se requiere iniciar sesión: la sesión no existe, venció o no es válida”;
**403** dice “Tu rol no tiene permiso para esta operación” o el motivo ABAC concreto. El filtro
`CorrelationIdFilter` corre antes que Spring Security, así que también los 401/403 llevan
`X-Trace-ID`.

## 6. SPA: almacenamiento del token y manejo de errores (S11)

| Decisión | Implementación |
|---|---|
| Dónde vive el token | Signal en memoria de `AuthService` con **copia espejo en `sessionStorage`** (`lamb_sesion`) para sobrevivir un F5 en la misma pestaña. **Nunca `localStorage`** (verificado en el navegador y en pruebas). |
| `code_verifier`, `state`, destino | `sessionStorage`, de un solo uso; se borran al completar el login |
| Login / logout | Redirección a Keycloak; RP-Initiated Logout con `id_token_hint` |
| 401 | `authInterceptor`: refresca con el `refresh_token` **una sola vez** y reintenta; si el refresco falla, nuevo login. Varios 401 simultáneos comparten una sola llamada de refresco (mejora sobre la guía). |
| 403 | Se propaga sin reintentar; la pantalla muestra el motivo del backend |
| Destino del token | Solo peticiones a `apiBaseUrl` (no viaja a otros hosts; mejora sobre la guía) |
| Guards | `authGuard` (sin sesión → Keycloak) y `rolGuard(...)` (rol insuficiente → `/sin-permiso`), siempre en ese orden |
| Menú y botones | Filtrados por rol con las mismas reglas de la matriz; son UX, la autorización real es la del backend |

Límite documentado (igual que S11 2.3): `sessionStorage` sigue siendo legible por un script del mismo
origen; un Backend-for-Frontend o la renovación silenciosa quedan fuera del alcance.

## 7. Operación

```powershell
# Una sola vez por equipo (claves locales; no se imprimen)
python -I infra/keycloak/generar_secretos.py
# Levantar Keycloak (Oracle no se toca)
cd infra; docker compose -f compose-dev.yml up -d keycloak-db keycloak
# Usuarios y atributo alumnoId (idempotente)
python -I infra/keycloak/sembrar_usuarios.py
# Verificaciones
python -I infra/pruebas/verificar_keycloak.py   # 8 casos: sin password grant, PKCE obligatorio, claims, refresco
python -I infra/pruebas/matriz_accesos.py       # 22 casos reales 401/403/200 contra el backend
```

La consola de administración está en `http://localhost:48080` (usuario `admin`, clave en
`infra/.env`). `start-dev` no es apto para producción: falta TLS y hostname (S10b 9).

## 8. Claves temporales para la presentación (solo local)

Para iniciar sesión rápido frente al docente, los cinco usuarios de prueba (`ana.quispe`,
`jose.apaza`, `coordinador`, `admin.lamb`, `cajero`) pueden usar claves distintas y fáciles de
escribir (`Palabra-Palabra-NNN`). No se tocan la cuenta administradora de Keycloak (`admin` del realm
`master`), las credenciales de Oracle ni ningún otro Keycloak (por ejemplo, el del puerto 38080).

```powershell
# Genera las claves de demostración (respalda antes las vigentes en keycloak-usuarios.anterior.env)
python -I infra/keycloak/claves_demostracion.py
# Las aplica en el Keycloak local (48080) con el mecanismo administrativo existente
python -I infra/keycloak/sembrar_usuarios.py
# Comprueba el inicio de sesión PKCE y los roles de los cinco usuarios
python -I infra/pruebas/verificar_keycloak.py
```

Las claves solo están en `infra/secretos/keycloak-usuarios.env` (ignorado por Git); ningún script
las imprime. Ese archivo no se proyecta durante la exposición.

**Después de la presentación**, vuelve a claves largas aleatorias (recomendado) o a las anteriores:

```powershell
python -I infra/keycloak/claves_demostracion.py --nuevas      # 20 caracteres aleatorios por usuario
# o: python -I infra/keycloak/claves_demostracion.py --restaurar  (claves previas a la demo)
python -I infra/keycloak/sembrar_usuarios.py
python -I infra/pruebas/verificar_keycloak.py
```
