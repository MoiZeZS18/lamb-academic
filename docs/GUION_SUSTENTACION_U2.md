# Guion de sustentación individual · Unidad II · Matrícula

Tiempos de S12 (Tabla 3): presentación 8 min, demo 5 min, preguntas 5 min.

## Antes de empezar (5 min antes)

1. `docker ps`: `lamb-academic-oracle`, `lamb-academic-keycloak`, `lamb-academic-keycloak-db` en ejecución.
2. Backend: `cd backend; java -jar target\lamb-academic-0.0.1-SNAPSHOT.jar` → `http://localhost:8080/actuator/health` = UP.
3. Frontend: `cd frontend; npm.cmd start` → `http://localhost:4200`.
4. Ventana de incógnito (sin sesión previa de Keycloak). Claves de prueba a mano en
   `infra/secretos/keycloak-usuarios.env` (no proyectar ese archivo).
5. Para mostrar el refresco por expiración real: en la consola de Keycloak (`localhost:48080`, realm
   `lamb-academic` → Realm settings → Tokens) bajar temporalmente *Access Token Lifespan* a 1 min y
   restaurarlo a 5 min al terminar.

## Presentación (8 min)

1. **Organización (1,5 min)**: `core/shared/features`, layout con `router-outlet`, rutas perezosas,
   servicios por recurso (`docs/FRONTEND_MATRICULA.md` §1).
2. **Arquitectura de seguridad (2 min)**: diagrama de `docs/SEGURIDAD_KEYCLOAK.md` §1. Keycloak emite,
   el backend valida (issuer-uri), la SPA nunca ve la contraseña.
3. **Usuario ↔ Alumno (1,5 min)**: atributo `alumnoId` solo editable por el admin → claim
   `alumno_id` → `IdentidadToken`. Por qué no se toma del request.
4. **Matriz de accesos (1,5 min)**: §5 de seguridad; menú y botones reflejan la misma matriz.
5. **Pruebas (1,5 min)**: 104 backend, 67 frontend, 22 de matriz, 101 E2E, huella de Oracle intacta.

## Demo (5 min)

1. Abrir `/matricula/inscripcion` sin sesión → redirige a Keycloak (mostrar la URL con
   `code_challenge_method=S256`). Entrar como `jose.apaza`.
2. DevTools → Application: `sessionStorage` solo `lamb_sesion`; `localStorage` vacío.
3. **Cabecera-detalle**: elegir IS101 e IS104 → cruce marcado y envío bloqueado; quitar IS104, elegir
   IS102 e IS201 → registrar → **409 real** “Para matricularse en IS201 debe haber aprobado antes
   IS101”. Quitar IS201 → confirmar → matrícula con su cargo (S/ 550, 5 cuotas).
4. **403**: cambiar la URL a `/matricula/consulta/93` → “No tienes acceso a la matrícula de otro
   estudiante”. En Network: un solo 403, sin refresco.
5. **401**: esperar la expiración (1 min) y abrir “Matrículas” → Network: 401, POST `/token` con
   `grant_type=refresh_token`, reintento 200.
6. Cerrar sesión; entrar como `coordinador`: aparece Reportes y el botón **Anular**. Anular la
   matrícula de José (cupos liberados, cargo anulado).
7. **CRUD dependiente**: Secciones → Nueva → guardar sin curso (mensaje, sin petición); autocomplete;
   docente con cruce → 409 real; guardar en sábado; eliminar IS101-A → 409 “ya tiene matrículas”.
8. Cerrar con la fila de integración de `lp2-demo-matricula.md` §5 (GET /matriculas/94 con dos roles).

Después de la demo, para dejar los datos como estaban:
`python -I infra/pruebas/limpiar_matricula_prueba.py <id de la matrícula anulada>` y borrar la sección
de prueba desde la propia SPA.

## Preguntas de referencia (S12 Tabla 2) y respuestas cortas

1. **¿Por qué SeccionForm carga cursos desde CursoService?** La lista sale de los datos reales: un
   curso nuevo aparece solo y un curso inactivo no se ofrece; escribirla a mano la desactualiza.
2. **¿Qué impide enviar una matrícula sin líneas?** `alMenosUnaLinea` en el FormArray y la
   verificación `form.invalid` antes del diálogo; el backend además exige `detalles` no vacío (400).
3. **¿Por qué el alumno se lee de `alumno_id` del token y no del request?** El token está firmado por
   Keycloak y validado por el backend; un campo del request lo cambia cualquiera con DevTools.
4. **¿Qué cambia si se reemplaza Keycloak?** `issuer-uri`, el nombre del claim de roles en el
   conversor y el claim `alumno_id`; controladores y servicios no cambian.
5. **¿Por qué el `code_verifier` va a `sessionStorage`?** La redirección completa a Keycloak destruye
   la memoria de la SPA; el verifier sobrevive esa recarga, es de un solo uso y se borra al usarse.
6. **¿Qué se pierde con memoria en vez de `localStorage`?** La sesión no se comparte entre pestañas
   ni sobrevive cerrar el navegador; es aceptable porque reduce la ventana de exposición.
7. **¿Qué escondería un reintento sin límite ante 401?** Un token rechazado por otra causa (firma,
   emisor, usuario deshabilitado) y saturaría Keycloak y el backend; por eso hay un solo reintento.
