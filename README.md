# LAMB Academic

Sistema académico modular para la gestión universitaria: backend REST único (monolito modular
Spring Boot 4.0.8 + Spring Modulith, Java 21) conectado a Oracle, protegido con Keycloak, y SPA
Angular 22 del módulo de Matrícula (Unidad II).

## Módulos
- **Base Universal**: institucional (sede, facultad, escuela), personas, alumnos, expedientes y documentos.
- **Core Académico**: cursos, secciones y horarios (oferta), malla curricular y prerrequisitos, plana docente.
- **Matrícula**: periodos y registro transaccional de matrícula (cupos, cruces, duplicados, prerrequisitos).
- **Finanzas del Estudiante**: conceptos de pago, cargos con cuotas, pagos y saldos.

Documentación: [ARQUITECTURA_LAMB.md](ARQUITECTURA_LAMB.md) · [MATRIZ_REQUISITOS.md](MATRIZ_REQUISITOS.md) ·
[RESULTADOS_PRUEBAS.md](RESULTADOS_PRUEBAS.md) · [ESTADO_INTEGRACION.md](ESTADO_INTEGRACION.md) ·
[PENDIENTES.md](PENDIENTES.md) · [CONTRATOS_REST.md](CONTRATOS_REST.md) ·
[MODULOS_LAMB.md](MODULOS_LAMB.md)

Unidad II: [docs/SEGURIDAD_KEYCLOAK.md](docs/SEGURIDAD_KEYCLOAK.md) ·
[docs/FRONTEND_MATRICULA.md](docs/FRONTEND_MATRICULA.md) · [docs/lp2-demo-matricula.md](docs/lp2-demo-matricula.md) ·
[docs/TRAZABILIDAD_U2.md](docs/TRAZABILIDAD_U2.md) · [docs/GUION_SUSTENTACION_U2.md](docs/GUION_SUSTENTACION_U2.md)

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
   docker compose -f compose-dev.yml up -d oracle
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

## Keycloak y el frontend de Matrícula (Unidad II)

Requisitos adicionales: Node 24 y npm, Python 3. Desde la API, todas las rutas (salvo salud y Swagger)
exigen un token de Keycloak: levantar Keycloak **antes** del backend.

1. **Claves locales de Keycloak** (una vez; no se imprimen, quedan en `infra/.env` e
   `infra/secretos/keycloak-usuarios.env`, ambos ignorados por Git):
   ```powershell
   python -I infra/keycloak/generar_secretos.py
   ```
2. **Keycloak** (contenedores aparte, no tocan Oracle) y usuarios de prueba:
   ```powershell
   cd infra
   docker compose -f compose-dev.yml up -d keycloak-db keycloak
   cd ..
   python -I infra/keycloak/sembrar_usuarios.py
   python -I infra/pruebas/verificar_keycloak.py
   ```
   Consola: http://localhost:48080 (usuario `admin`, clave `KC_ADMIN_PASSWORD` de `infra/.env`).
   Usuarios: `ana.quispe`, `jose.apaza` (ESTUDIANTE), `coordinador`, `admin.lamb`, `cajero`; sus claves
   están en `infra/secretos/keycloak-usuarios.env`.
3. **Backend** (pasos 4-5 de arriba). Verificación de la matriz de accesos:
   `python -I infra/pruebas/matriz_accesos.py`.
4. **Frontend**:
   ```powershell
   cd frontend
   npm.cmd install
   npm.cmd start
   ```
   Abrir http://localhost:4200 e iniciar sesión (la pantalla de login es la de Keycloak).
   Pruebas: `npx ng test --watch=false` · compilación: `npx ng build`.
   (`npm.cmd` en vez de `npm` si PowerShell bloquea scripts `.ps1`.)
