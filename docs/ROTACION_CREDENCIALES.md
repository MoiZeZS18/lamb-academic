# Auditoría de credenciales y procedimiento de rotación (PENDIENTES A4)

> Procedimiento preparado, **no ejecutado**. Cambiar contraseñas o reescribir el historial
> requiere autorización explícita del responsable del proyecto.

## 1. Hallazgos de la auditoría (2026-10-24)

| # | Hallazgo | Evidencia | Riesgo |
|---|---|---|---|
| 1 | La clave de `LAMB_APP` está en texto plano en `backend/src/main/resources/application-dev.yml` del commit `4e5beab` | `git log --all -S'<clave_filtrada>'` → `4e5beab` (la introduce) y `de5a4a5` (la retira); ningún otro commit la contiene | Cualquiera con acceso al repositorio la conoce |
| 2 | Ese commit **ya está publicado** en `origin/main` (`https://github.com/MoizeZS18/lamb-academic.git`) | `git rev-list --left-right --count origin/main...main` → `0 0` | Acceso a la base desde quien clone el repositorio |
| 3 | El repositorio responde 404 sin autenticación: aparenta ser **privado** (no se pudo confirmar con `gh`, no instalado) | `curl https://github.com/MoizeZS18/lamb-academic` → 404 | Exposición limitada a colaboradores, si es privado |
| 4 | `SYS`/`SYSTEM` usan el mismo valor débil que `LAMB_APP` | `infra/.env` (no versionado) y el `compose-dev.yml` original | Control total de la base con la misma clave |
| 5 | Oracle escucha en **todas las interfaces** (`0.0.0.0:1522` e IPv6) | `docker port lamb-academic-oracle` | Accesible desde la red local, no solo desde este equipo |
| 6 | Desde la integración ningún archivo versionado contiene claves | revisión de los commits `de5a4a5`, `56243e4`, `1bcd817` y del commit de esta auditoría | — |
| 7 | La carpeta duplicada `lamb-academic/` (no versionada) contiene el `compose-dev.yml` original con claves en texto plano | `lamb-academic/infra/compose-dev.yml` | Solo local; está en `.gitignore` |

Comportamiento comprobado de la imagen `gvenzl/oracle-free` (`/opt/oracle/container-entrypoint.sh`):
con la base ya inicializada, `ORACLE_PASSWORD` **se ignora** (línea 463, advertencia
«The password will be ignored») y `APP_USER`/`APP_USER_PASSWORD` solo se usan en la primera
inicialización. Por eso **cambiar `infra/.env` no cambia ninguna clave**: la rotación se hace dentro de Oracle.

## 2. Procedimiento de rotación sin perder acceso

Ejecutar desde PowerShell en `D:\LP2\lamb-academic`. Elegir claves nuevas fuertes y no escribirlas
en archivos versionados.

1. **Respaldo previo** (no destructivo):
   ```powershell
   docker exec lamb-academic-oracle expdp system/<clave_system_actual>@localhost:1521/FREEPDB1 schemas=LAMB_APP directory=DATA_PUMP_DIR dumpfile=lamb_app_pre_rotacion.dmp logfile=lamb_app_pre_rotacion.log
   ```
2. **Detener el backend** (evita conexiones del pool con la clave vieja durante el cambio).
3. **Rotar `LAMB_APP`** conectado como `system`:
   ```sql
   ALTER USER LAMB_APP IDENTIFIED BY "<clave_nueva_app>";
   ```
4. **Verificar el acceso nuevo antes de tocar nada más**:
   ```powershell
   echo "SELECT USER FROM dual;" | docker exec -i lamb-academic-oracle sqlplus -s "LAMB_APP/<clave_nueva_app>@localhost:1521/FREEPDB1"
   ```
   Si falla, volver al paso 3 con la clave anterior (reversión inmediata).
5. **Actualizar `backend/.env`** (`LAMB_DB_PASSWORD=<clave_nueva_app>`) y comprobar:
   `cd backend; .\mvnw.cmd test -Dtest=LambAcademicApplicationTests` → 1 prueba en verde.
6. **Rotar `SYS`/`SYSTEM`** con el comando de la propia imagen:
   ```powershell
   docker exec lamb-academic-oracle resetPassword "<clave_nueva_system>"
   ```
   Verificar con `sqlplus system/<clave_nueva_system>@localhost:1521/FREEPDB1`.
7. **Actualizar `infra/.env`** con las claves nuevas (solo documenta el valor vigente y sirve para
   una base nueva). Atención: cambia el hash de configuración de Compose, por lo que el próximo
   `docker compose up -d` **recreará el contenedor**; el volumen `lamb-academic-dev_oracle-data` se
   conserva y, como la base ya existe, las claves no cambian. Comprobar antes con
   `docker compose -f infra/compose-dev.yml config --hash oracle`. Nunca usar `down -v`.
8. **Repartir las claves nuevas** al equipo por un canal privado (no por el repositorio).

## 3. Historial de Git

- **Recomendación: no reescribir el historial.** Tras rotar, el valor del commit `4e5beab` deja de
  ser válido y el riesgo desaparece. Reescribirlo (`git filter-repo`, BFG) cambia todos los hashes,
  exige `push --force` sobre `main` y rompe los clones del equipo y de la carpeta duplicada.
- Solo si el repositorio fuera público y la clave no pudiera rotarse, reescribir con autorización,
  avisando a todo el equipo para que vuelva a clonar.

## 4. Endurecimiento recomendado (requiere autorización: recrea el contenedor)

- Publicar Oracle solo en el equipo local: en `infra/compose-dev.yml`, `ports: - "127.0.0.1:1522:1521"`.
  Al aplicarlo, Compose recrea el contenedor conservando el volumen.
- Usar claves distintas para `SYSTEM` y `LAMB_APP`.
