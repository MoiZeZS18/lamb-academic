# LAMB Academic

Sistema académico modular para la gestión universitaria: backend REST único (monolito modular
Spring Boot 4.0.8 + Spring Modulith, Java 21) conectado a Oracle.

## Módulos
- **Base Universal**: institucional (sede, facultad, escuela), personas, alumnos, expedientes y documentos.
- **Core Académico**: cursos, secciones y horarios (oferta), malla curricular y prerrequisitos, plana docente.
- **Matrícula**: periodos y registro transaccional de matrícula (cupos, cruces, duplicados, prerrequisitos).
- **Finanzas del Estudiante**: conceptos de pago, cargos con cuotas, pagos y saldos.

Documentación: [ARQUITECTURA_LAMB.md](ARQUITECTURA_LAMB.md) · [MATRIZ_REQUISITOS.md](MATRIZ_REQUISITOS.md) ·
[RESULTADOS_PRUEBAS.md](RESULTADOS_PRUEBAS.md) · [ESTADO_INTEGRACION.md](ESTADO_INTEGRACION.md) ·
[PENDIENTES.md](PENDIENTES.md) · [CONTRATOS_REST.md](CONTRATOS_REST.md) ·
[MODULOS_LAMB.md](MODULOS_LAMB.md)

## Levantar el backend (Windows, PowerShell)

Requisitos: Java 21, Docker Desktop. Maven no es necesario (se usa `mvnw.cmd`).

1. **Credenciales locales** (no se versionan; nunca las publiques):
   - copiar `infra/.env.example` a `infra/.env` y completar las claves del contenedor;
   - copiar `backend/.env.example` a `backend/.env` y completar `LAMB_DB_PASSWORD`;
   - activar el bloqueo de secretos en el clon: `git config core.hooksPath .githooks`.
   Oracle solo escucha en `127.0.0.1:1522`. Rotación de claves: `docs/ROTACION_CREDENCIALES.md`.
2. **Oracle** (si no está levantado). Nunca usar `down -v`: borra el volumen con los datos.
   ```powershell
   cd infra
   docker compose -f compose-dev.yml up -d
   ```
3. **Esquema** (solo en una base nueva; la base DEV actual ya los tiene). Primero un respaldo
   `expdp`; luego, conectado como `LAMB_APP`, ejecutar en orden `infra/oracle/V2__integracion_modulos.sql`,
   (opcional) `infra/oracle/V3__datos_demo.sql` y `infra/oracle/V4__resultado_detalle_matricula.sql`.
4. **Pruebas** (requieren Oracle levantado):
   ```powershell
   cd backend
   .\mvnw.cmd test
   ```
5. **Arranque**:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   - Salud: http://localhost:8080/actuator/health
   - Swagger UI: http://localhost:8080/swagger-ui.html · OpenAPI: http://localhost:8080/v3/api-docs
   - Módulos: http://localhost:8080/actuator/modulith

6. **Pruebas HTTP repetibles** (con el backend levantado; crean y borran sus propios datos):
   ```powershell
   python -I infra/pruebas/e2e_repetible.py
   ```

El backend debe ejecutarse desde la carpeta `backend/` para que lea `backend/.env`; también acepta
las variables de entorno `LAMB_DB_URL`, `LAMB_DB_USERNAME` y `LAMB_DB_PASSWORD`.
