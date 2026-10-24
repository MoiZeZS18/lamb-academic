# Pendientes y decisiones abiertas — LAMB Academic Backend

## A. Decisiones que necesitan al equipo / al usuario

| # | Tema | Situación actual | Decisión necesaria |
|---|---|---|---|
| A1 | **Prerrequisitos: "llevado" vs "aprobado"** | No existe módulo de notas en ningún aporte. La regla considera cumplido un prerrequisito si el curso figura en una matrícula no anulada de un periodo que terminó antes (`lamb.matricula.validar-prerrequisitos=true`). | ¿Se crea un registro de notas/actas (U2) o se acepta esta aproximación para U1? Se puede desactivar con la propiedad. |
| A2 | **Matrículas de prueba previas** | 7 matrículas del estudiante 1 en 2026-II (6 duplicadas), estado `INICIADA`, con secciones 1, 2, 100, 101 que nunca existieron. Se conservaron intactas (`CLAVE_ACTIVA` NULL, no ocupan cupos). | ¿Anularlas (vía `PATCH /api/v1/matriculas/{id}/anular`) o eliminarlas? Eliminar es irreversible y requiere autorización explícita. |
| A3 | **Carpeta duplicada `lamb-academic/lamb-academic/`** | Copia exacta de `backend/`, `infra/` y `README.md`; ignorada por Git, no se tocó. | ¿Borrarla? (operación del usuario). |
| A4 | **Contraseña en el historial de Git** | `application-dev.yml` con la clave del Oracle DEV quedó en el commit `4e5beab` (anterior a esta integración). Desde ahora se lee de `backend/.env`. | Si el repositorio es público, cambiar la clave del contenedor DEV. No se reescribió el historial (operación destructiva). |
| A5 | **Unir la rama** | Todo está en `integracion/backend-unificado` (sin push). | Revisar y fusionar en `main`; hacer push. |
| A6 | **Contratos que cambiaron respecto a los frontends de los compañeros** | Persona usa el modelo completo (`tipoDocumento`, `numeroDocumento`, apellidos separados) en lugar de `dni/nombres/apellidos`; documentos en `/api/v1/expedientes/{id}/documentos` (antes `/api/v1/documento-adjunto`); errores con `message` (antes `mensaje` en Matrícula y `error` en otros); Matrícula responde 404/409 donde antes respondía 400. | Aceptar los contratos nuevos (documentados en OpenAPI) al construir el frontend en U2. |
| A7 | **Autoría** | El código de los compañeros fue adaptado (paquetes, DTO, excepciones, reglas). | Cada responsable debería revisar su módulo para poder sustentarlo (S6, preguntas individuales). |

## B. Trabajo pendiente técnico

| # | Pendiente | Prioridad |
|---|---|---|
| B1 | `lp2-demo.md` (producto de la Unidad I, S6 tabla 4): la plantilla del docente no está en el material recibido. Se puede armar con `ARQUITECTURA_LAMB.md`, `MATRIZ_REQUISITOS.md` y `RESULTADOS_PRUEBAS.md`. | Alta |
| B2 | Prueba de CORS en navegador real (`fetch` desde `python -m http.server 5500`, S5 3.8) durante la demo. | Media |
| B3 | Objetos PL/SQL de BD2 del aporte LP2_G1 (`FN_TOTAL_CREDITOS_PLAN`, `TRG_DETALLE_PLAN_TOTALES`, `SP_REGISTRAR_PLAN_ESTUDIO`, `TRG_AUDITORIA_PERSONA`…): no se aplicaron porque sus scripts incluyen `DROP TABLE`. Las tablas usan los mismos nombres; habría que extraer solo funciones/triggers, revisar que no dupliquen los cálculos del backend y aplicar. | Media |
| B4 | Seguridad JWT (aporte LP2_L) — corresponde a U2 (S10). | U2 |
| B5 | Estado de matrícula es texto libre (contrato original); evaluar enum (`INICIADA`, `CONFIRMADA`, `ANULADA`). | Baja |
| B6 | Las pruebas de integración usan el esquema DEV (se autolimpian). Evaluar un esquema de pruebas separado. | Baja |
| B7 | Escalamiento con dos instancias (S1 3.3) no probado en esta sesión. | Baja |
| B8 | Archivos adjuntos: se guardan metadatos y ruta, no el binario (igual que el aporte original). | U2 |
| B9 | Columnas `ACTIVO NUMBER(1)`: se mantienen por compatibilidad con BD2; si BD2 migra a `BOOLEAN` nativo de 23ai, retirar `BooleanNumeroConverter`. | Baja |

## C. Riesgos conocidos

- Las pruebas HTTP (`infra/pruebas/e2e_demo.py`) crean datos de demostración y no son repetibles sin limpiarlos.
- `V2__rollback.sql` borra las tablas nuevas **y sus datos**; solo debe ejecutarse con autorización y tras un respaldo.
