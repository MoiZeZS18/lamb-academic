# Informe final · Unidad II · Frontend de Matrícula, Keycloak e integración

Rama `unidad2/frontend-matricula` (sin push ni fusión con `main`). Fecha: 2026-10-24.

## 1. Qué se implementó

- **Keycloak 26.8** en contenedores propios (`lamb-academic-keycloak` y su Postgres), sin tocar
  Oracle. Realm `lamb-academic` en español, roles ESTUDIANTE, COORDINADOR, ADMIN y CAJERO, cliente
  público `lamb-frontend` con Authorization Code + PKCE obligatorio y sin Direct access grants, y el
  claim `alumno_id` que vincula a cada estudiante con su `ALUMNOS.ID` real (solo el administrador
  puede cambiarlo).
- **Backend protegido** como Resource Server: matriz de permisos por rol, filtro por dueño para el
  estudiante (403 con el motivo), errores 401/403 con el formato común del backend y `X-Trace-ID`.
- **SPA Angular 22** (`frontend/`): portal de matrícula, inscripción con oferta y formulario
  cabecera-detalle, consulta paginada, detalle con cargo y cuotas, horario semanal, reportes, CRUD de
  Periodo (S7) y CRUD de Sección (S8).

## 2. Qué se reutilizó del proyecto de Cristian

El servicio de avisos, el patrón de menú declarativo por roles y el validador de curso repetido del
FormArray. Se descartaron su JWT propio, su formulario de login, el token en `localStorage` y la
clave privada incluida. Detalle en `docs/FRONTEND_MATRICULA.md` §3.

## 3. Qué se modificó del backend

- `SecurityConfig`, `CorsConfig` (como bean de Spring Security), `OpenApiConfig` (candado de Swagger),
  `PerfilController` (`GET /api/v1/auth/perfil`), `IdentidadToken`, `AccesoDenegadoException`.
- Filtro por dueño en `MatriculaController`, `MatriculaService.obtener` y `CargoController`.
- `CorrelationIdFilter` corre antes que Spring Security (los 401/403 llevan `X-Trace-ID`).
- **Nuevo `DELETE /api/v1/secciones/{id}`** mediante `SeccionEnUsoPort`; requiere la revisión del
  responsable de Core Académico: `docs/REVISION_DELETE_SECCIONES.md`.
- Sin cambios de esquema ni de datos en Oracle.

## 4. Pantallas completas

Inicio, portal de matrícula, inscripción, consulta, detalle, horario, reportes, periodos (lista y
formulario), secciones (lista y formulario), retorno de login, acceso no permitido y página no
encontrada. Todas en español y conectadas al backend real.

## 5. Funcionalidades conectadas a Oracle

Todas: periodos, secciones con horarios y vacantes, cursos, docentes, alumnos, registro transaccional
de la matrícula con reserva de cupos, cargo financiero con cuotas generado en la misma transacción,
anulación (libera cupos y anula el cargo), resultados de curso y reportes agregados.

## 6. Roles que funcionan en Keycloak

`ana.quispe` y `jose.apaza` (ESTUDIANTE, vinculados a los alumnos 1001 y 1033), `coordinador`,
`admin.lamb` y `cajero`. Login real con PKCE verificado para los cinco.

## 7. Pruebas ejecutadas (última ejecución, revisión de idioma)

| Prueba | Resultado |
|---|---|
| Backend `mvnw clean package` | 104/104 (incluye `ModularityTests` 3/3 y `SecurityConfigTest` 13/13) |
| Frontend `ng test` | 67/67 en 15 archivos |
| Compilación de producción `ng build` | Correcta (88 kB iniciales comprimidos) |
| `verificar_keycloak.py` | 8/8 |
| `matriz_accesos.py` (tokens reales) | 22/22 |
| `e2e_repetible.py` | 101/101, conteos de las 19 tablas iguales al inicio |
| Huella de datos de Oracle | Idéntica a la del cierre de seguridad |
| Navegador | Login y logout reales, token solo en `sessionStorage`, 401 con un único refresco, 403 por dueño y por rol, cabecera-detalle con error real del backend, ambos CRUD, anulación |

## 8. Requisitos de S12 cumplidos

Todos los criterios mínimos de aceptación de S12 §4 tienen evidencia (`docs/lp2-demo-matricula.md`
§3 y §6). La espera real de 5 minutos para la expiración del token queda para la sustentación (se
recomienda bajar temporalmente la vida del token a 1 minuto).

## 9. Pendientes

Ver `PENDIENTES.md` §E: evidencias con reloj y usuario tomadas por el estudiante; revisión del
DELETE de Secciones por Core Académico; configuración de Keycloak en el equipo de cada compañero;
plantilla oficial `lp2-demo.md`; rol DOCENTE; edición de horarios de una sección programada;
Keycloak en modo producción con TLS.

## 10. Cómo abrir el frontend

1. `cd infra; docker compose -f compose-dev.yml up -d oracle keycloak-db keycloak`
2. `cd backend; java -jar target\lamb-academic-0.0.1-SNAPSHOT.jar` (o `.\mvnw.cmd spring-boot:run`)
3. `cd frontend; npm.cmd start`
4. Abrir `http://localhost:4200` e iniciar sesión con un usuario de prueba (claves en
   `infra/secretos/keycloak-usuarios.env`).

Instrucciones completas en `README.md`.
