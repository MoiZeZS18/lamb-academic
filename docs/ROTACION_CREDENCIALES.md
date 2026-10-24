# Seguridad de credenciales: auditoría, rotación y saneamiento del historial

## 0. Ejecución (2026-10-24) — D1, D2, D3 y D6 aplicados; D4 no autorizado; D5 pospuesto

| Etapa | Resultado verificado |
|---|---|
| Comprobaciones previas | Sin backend en 8080; 0 sesiones de `LAMB_APP`; clientes del listener: solo este equipo (`DESKTOP-39GJ1J9`) y el propio contenedor |
| Respaldo previo | `lamb_app_pre_rotacion_*.dmp` (19 tablas, `sqlfile` legible); directorio Data Pump completo copiado a `infra/backups/contenedor-dpdump/` (SHA-256 iguales); log del listener copiado |
| Huella de datos | `infra/oracle/verificacion/huella_datos.sql` (filas + hash de contenido de las 19 tablas, rol, cuota, objetos): **idéntica** antes de rotar, tras recrear el contenedor y al final |
| D1 Rotación | `infra/oracle/verificacion/rotar_credenciales.py`: 4 claves distintas de 24 caracteres generadas con `secrets`, guardadas antes de aplicarse en `infra/secretos/oracle.env` (ignorado; ACL solo del usuario). `LAMB_APP` y `PDBADMIN` en `FREEPDB1`; `SYSTEM` y `SYS` en `CDB$ROOT`. Cada una: **nueva conecta, antigua `ORA-01017`** |
| Configuración local | `backend/.env` (`LAMB_DB_PASSWORD`) e `infra/.env` (`APP_USER_PASSWORD`, `ORACLE_PASSWORD` = `SYSTEM`) actualizados; ACL solo del usuario |
| D3 Contenedor | `compose-dev.yml`: puerto `127.0.0.1:1522`, imagen fijada por digest (la misma en ejecución, Oracle 23.26.2), `stop_grace_period: 2m`. Recreado: mismo volumen, «database already initialized», variables actualizadas, 0 reinicios. La raíz del CDB necesitó la recuperación automática de instancia (el contenedor anterior terminó por `SIGTERM`, código 143); `FREEPDB1` estaba cerrada limpiamente y la huella confirma los datos. **Próxima vez:** `SHUTDOWN IMMEDIATE` explícito antes de recrear |
| D2 Hook | `git config core.hooksPath .githooks`: bloquea la clave vigente en un archivo preparado; deja pasar cambios legítimos |
| D6 Insumos | `D:\LP2\.git\info\exclude` (local, no versionado): `LAMB-INTEGRACION-INSUMOS/`, `lamb-academic..zip`, `*.zip`; `git add -A --dry-run` ya no los incluye |
| D4 | No autorizado: historial sin reescribir; la clave del commit `4e5beab` quedó **invalidada** por la rotación |
| D5 | Pospuesto: los `verif_*.sql` se conservan (copiados al equipo antes de recrear el contenedor) |

Verificación posterior: `python -I infra/oracle/verificacion/verificar_credenciales.py` (solo lectura).
Los respaldos anteriores a la rotación contienen el verificador de la clave antigua: tras restaurar
uno en una base nueva, volver a ejecutar `rotar_credenciales.py` (reutiliza las claves guardadas).

> El resto de este documento es el procedimiento preparado antes de la ejecución.

> Ningún valor de clave aparece en este documento.

## 1. Credenciales en el historial Git (auditoría del 2026-10-24)

Barrido de **todos los blobs de todas las referencias** (`main`, `integracion/backend-unificado`,
`respaldo/main-antes-fusion`, `origin/main`) con patrones de claves, URLs con credenciales,
`usuario/clave@`, claves privadas y tokens, más búsqueda literal del valor conocido.

| Hallazgo | Detalle |
|---|---|
| Único secreto en el historial | `backend/src/main/resources/application-dev.yml`, línea 8 (`password:` con la clave de `LAMB_APP`), commit `4e5beab` |
| ¿Publicado? | **Sí.** `4e5beab` es `origin/main` en `https://github.com/MoizeZS18/lamb-academic.git` |
| ¿Quién lo contiene? | Las 4 referencias (todas descienden de `4e5beab`) |
| Commit que lo retira | `de5a4a5` (solo local, no publicado) |
| Mensajes de commit | 0 apariciones |
| Objeto inalcanzable local | 1 blob (`ec151fa`): versión preliminar de este documento con el valor, preparada y corregida antes del commit. **Nunca fue publicado** (los objetos inalcanzables no viajan en un push); Git lo elimina en su recolección automática |
| Visibilidad del repositorio | GitHub responde 404 sin autenticación: aparenta ser **privado** (no confirmado: `gh` no instalado) |
| Valor | La misma clave débil se usa para `LAMB_APP`, `SYSTEM`, `SYS` y `PDBADMIN` |

## 2. Secretos en otros archivos

| Lugar | ¿Versionado? | Estado |
|---|---|---|
| Archivos versionados actuales (todas las ramas) | Sí | **0 secretos** (solo `${LAMB_DB_PASSWORD}`, `${ORACLE_PASSWORD:?…}`, plantillas vacías `*.env.example` y variables vacías de `mvnw`) |
| `backend/.env`, `infra/.env` | No (ignorados) | Contienen las claves vigentes: correcto |
| Variables del contenedor (`docker inspect`) | No | `ORACLE_PASSWORD` y `APP_USER_PASSWORD` en texto plano; **no cambian con `ALTER USER`** |
| Respaldos Data Pump (`infra/backups/*.dmp` y en el contenedor) | No (ignorados) | Incluyen `CREATE USER LAMB_APP IDENTIFIED BY VALUES '<verificador>'`: restaurarlos en una base nueva recrearía el usuario con la **clave antigua** |
| `verif_*.sql` en `DATA_PUMP_DIR` del contenedor | No | Archivos de verificación generados en la integración; contienen el mismo verificador |
| Carpeta duplicada `lamb-academic/lamb-academic/` | No (ignorada) | `application-dev.yml` y `compose-dev.yml` con la clave en texto plano |
| Repositorio padre `D:\LP2` (otro remoto, `bomerp`) | **No ignorado** | `lamb-academic..zip` y `LAMB-INTEGRACION-INSUMOS/` (ZIP de los compañeros con credenciales) aparecen como sin seguimiento: un `git add -A` en `D:\LP2` los publicaría |

## 3. Datos de Oracle relevantes para rotar sin perder nada

| Comprobación | Resultado | Implicación |
|---|---|---|
| Usuarios | `LAMB_APP` y `PDBADMIN` (locales de `FREEPDB1`), `SYS` y `SYSTEM` (comunes); todos `OPEN`, perfil `DEFAULT` | — |
| Política de claves (`DEFAULT`) | Sin función de verificación; reutilización ilimitada; `PASSWORD_LIFE_TIME` ilimitado; `PASSWORD_ROLLOVER_TIME = 0` | Se puede volver a la clave anterior si algo falla |
| Privilegios de `LAMB_APP` | `DB_DEVELOPER_ROLE`, cuota ilimitada en `USERS`, 88 objetos | `ALTER USER … IDENTIFIED BY` **no altera** roles, cuotas ni objetos |
| Sesiones abiertas como `LAMB_APP` | 0 | Sin conexiones que cortar |
| DB links / credenciales almacenadas | 0 / 0 | Nada interno depende de la clave |
| Script `resetPassword` de la imagen | Ejecuta `ALTER USER SYS` y `ALTER USER SYSTEM` | **No cambia `PDBADMIN`**: hay que rotarlo aparte |
| `ORACLE_PASSWORD` en un contenedor ya inicializado | Se ignora (`container-entrypoint.sh`, línea 463) | Cambiar `infra/.env` **no cambia** ninguna clave |

## 4. Procedimiento de rotación recomendado

Equipo: PowerShell en `D:\LP2\lamb-academic`, Docker Desktop activo. El backend detenido.
Las claves se escriben **solo** en los prompts ocultos de SQL*Plus y en los `.env`: nunca en la
línea de comandos (quedaría en el historial de PowerShell y en la lista de procesos).

**Paso 0. Claves nuevas.** Generar dos valores distintos (uno para `LAMB_APP`, otro para
`SYS`/`SYSTEM`/`PDBADMIN`), de 24 caracteres, que empiecen con letra, solo letras y números
(evita problemas con `@` y `/` en `usuario/clave@servicio`). Guardarlos en un gestor de contraseñas.
```powershell
$c = -join ((65..90)+(97..122)+(48..57) | Get-Random -Count 23 | ForEach-Object {[char]$_}); "L$c"
```

**Paso 1. Respaldo previo** (solo lee datos; `expdp` pide la clave actual de `SYSTEM` sin mostrarla):
```powershell
docker exec -it lamb-academic-oracle expdp system@localhost:1521/FREEPDB1 schemas=LAMB_APP directory=DATA_PUMP_DIR dumpfile=lamb_app_pre_rotacion.dmp logfile=lamb_app_pre_rotacion.log
```
Copiarlo a `infra/backups/` (`docker cp`) y comparar su SHA-256 en host y contenedor.

**Paso 2. Foto de privilegios antes** (para compararla al final):
```sql
SELECT granted_role FROM dba_role_privs WHERE grantee = 'LAMB_APP';
SELECT COUNT(*) FROM dba_objects WHERE owner = 'LAMB_APP';
```

**Paso 3. Rotar `LAMB_APP`** con el comando `PASSWORD` de SQL*Plus (pide la clave sin mostrarla):
```powershell
docker exec -it lamb-academic-oracle sqlplus / as sysdba
```
```sql
ALTER SESSION SET CONTAINER = FREEPDB1;
PASSWORD LAMB_APP
```

**Paso 4. Verificar antes de seguir** (la clave antigua debe fallar con `ORA-01017`):
```powershell
docker exec -it lamb-academic-oracle sqlplus LAMB_APP@localhost:1521/FREEPDB1
```
Si la clave nueva no funciona: repetir el paso 3 con la clave anterior (reversión inmediata; la
política permite reutilizarla).

**Paso 5. Actualizar `backend/.env`** (`LAMB_DB_PASSWORD=<nueva>`) y verificar Spring Boot (sección 7).

**Paso 6. Rotar `SYS`, `SYSTEM` y `PDBADMIN`** (misma sesión `/ as sysdba`):
```sql
ALTER SESSION SET CONTAINER = CDB$ROOT;
PASSWORD SYSTEM
PASSWORD SYS
ALTER SESSION SET CONTAINER = FREEPDB1;
PASSWORD PDBADMIN
```
`/ as sysdba` dentro del contenedor usa autenticación del sistema operativo: si una clave se
olvida, se puede volver a fijar desde ahí, sin perder acceso.

**Paso 7. Actualizar `infra/.env`** (`ORACLE_PASSWORD`, `APP_USER_PASSWORD`). Solo documenta el
valor vigente para una base nueva. Cambia el hash de configuración de Compose (verificar con
`docker compose -f infra/compose-dev.yml config --hash oracle`): **no ejecutar `docker compose up -d`**
salvo que se autorice la recreación del contenedor (decisión D3). Nunca `down -v`.

**Paso 8. Respaldos con la clave antigua.** Tras restaurar cualquier `.dmp` anterior a la rotación,
ejecutar de nuevo el paso 3 (el dump trae el verificador antiguo). Mantener `infra/backups/` fuera del
repositorio (ya está ignorado). Opcional (decisión D5): eliminar los `verif_*.sql` del contenedor.

**Paso 9.** Compartir las claves nuevas con el equipo por un canal privado.

## 5. Evitar que el secreto vuelva a publicarse

Rotar invalida el valor, pero **no lo borra del historial**: `4e5beab` seguirá mostrándolo en
cualquier clon y en GitHub. Lo importante es que el valor deje de servir y que no reaparezca:

1. **`.gitignore` ampliado** (ya aplicado en el repo): cualquier `.env` en cualquier carpeta salvo
   `*.env.example`, y cualquier `*.dmp`.
2. **Hook `pre-commit` versionado** en `.githooks/pre-commit` (preparado; activar con
   `git config core.hooksPath .githooks`, decisión D2). Bloquea: archivos `.env`/`.dmp`/respaldos;
   **cualquier valor real** de las claves de `backend/.env` e `infra/.env` (los lee en el momento del
   commit, no los guarda); claves literales en `yml/properties/json/xml/sql/env`. No imprime valores.
   Probado en un clon desechable: 3 commits con secretos bloqueados (clave literal en
   `application-dev.yml`, `backend/.env` forzado con `-f`, valor en un documento) y 3 legítimos
   aceptados (documentación, configuración con `${LAMB_DB_PASSWORD}`, plantilla vacía).
   **Limitación conocida:** mientras la clave sea una secuencia corta y común, la regla del valor real
   bloqueará cualquier línea que la contenga (por ejemplo, un DNI de prueba que incluya esos dígitos).
   Con la clave nueva de 24 caracteres (paso 0) ese falso positivo desaparece; por eso conviene activar
   el hook **después** de rotar.
3. **Cada integrante** activa el hook en su clon (es configuración local; no viaja con el push).
4. **Repositorio padre `D:\LP2`**: agregar a su `.gitignore` `lamb-academic..zip` y
   `LAMB-INTEGRACION-INSUMOS/` (decisión D6; es otro proyecto).
5. Recomendado a futuro: un escaneo de secretos en CI (p. ej. gitleaks) cuando exista integración continua.

## 6. ¿Hace falta sanear el historial?

**Recomendación: no, si se rota la clave y el repositorio es privado.** Tras la rotación el valor
publicado no da acceso a nada. Sanear solo se justifica si el repositorio es o será público, o si la
clave no puede cambiarse.

### Ensayo realizado (copia desechable, sin tocar el repositorio real)

`git filter-branch` sobre una copia del repositorio, reemplazando la línea por `${LAMB_DB_PASSWORD}`:

| Commit | Hash nuevo |
|---|---|
| `cbbb88a` | igual (anterior al secreto) |
| `4e5beab`, `de5a4a5`, `56243e4`, `1bcd817`, `c707f36`, `ce38c37` | **todos cambian** (6 commits) |

- Árbol final idéntico al actual: **sí** (no se pierde ningún archivo del trabajo de los cinco integrantes).
- Apariciones del valor en las ramas reescritas: **0**. Las referencias remotas `origin/*` de la copia
  seguían conteniéndolo: el saneamiento solo es efectivo si también se reemplaza `origin/main`.

### Riesgos

| Riesgo | Consecuencia |
|---|---|
| Cambian 6 hashes, incluido `origin/main` | Exige `git push --force-with-lease`; todo clon que contenga `4e5beab` (los de los compañeros) queda divergente y debe volver a clonarse. La carpeta duplicada está en `cbbb88a`, anterior al secreto, y no se ve afectada |
| Trabajo no publicado de los compañeros basado en `4e5beab` | Puede perderse si hacen `reset` sin respaldarlo; deben exportarlo antes (`git bundle` o `format-patch`) |
| Referencias a hashes en la documentación | `ESTADO_INTEGRACION.md`, `PENDIENTES.md`, `RESULTADOS_PRUEBAS.md` y este documento citan `4e5beab`, `de5a4a5`, `c707f36`…: quedarían desactualizados |
| GitHub conserva el commit viejo | Accesible por su SHA, en caché, en forks o en referencias de PR hasta su recolección; eliminarlo del todo requiere solicitarlo al soporte de GitHub |
| Herramienta | `git filter-repo` (recomendada) no está instalada; `filter-branch` funciona pero está desaconsejada |

### Procedimiento (solo con autorización D4)

1. Rotar primero (sección 4): sanear sin rotar no protege nada.
2. Pedir a los compañeros que publiquen o exporten su trabajo y dejen de hacer push.
3. Respaldo completo y verificado:
   ```powershell
   git bundle create ..\respaldo-lamb-academic-AAAAMMDD.bundle --all
   git bundle verify ..\respaldo-lamb-academic-AAAAMMDD.bundle
   git clone --mirror . ..\respaldo-lamb-academic-AAAAMMDD.git
   ```
4. Instalar `git filter-repo` (`py -m pip install git-filter-repo`) y trabajar en un clon espejo nuevo.
   Crear **fuera del repositorio** `reemplazos.txt` con una línea `regex:password:\s*<valor>==>password: ${LAMB_DB_PASSWORD}`
   y ejecutar `git filter-repo --replace-text ..\reemplazos.txt`. Borrar `reemplazos.txt` al terminar.
5. Verificar en el clon saneado: 0 apariciones del valor en `git rev-list --all`; `git diff <ce38c37> <main nuevo>` vacío;
   `.\mvnw.cmd clean test` 83/83.
6. Publicar (decisión D4): `git push --force-with-lease origin main` y, si se publicaran, las demás ramas.
7. Todos los integrantes vuelven a clonar; actualizar las referencias a hashes en la documentación.
8. Recuperación si algo sale mal: `git clone ..\respaldo-lamb-academic-AAAAMMDD.bundle` restaura todas las
   ramas; para el remoto, `git push --force origin <rama>` desde ese clon restablece el estado anterior.

## 7. Cambios en `.env` y verificación posterior

| Archivo | Clave | Cuándo |
|---|---|---|
| `backend/.env` | `LAMB_DB_PASSWORD` | Paso 5 (obligatorio: el backend, las pruebas y `e2e_repetible.py` la leen de aquí) |
| `infra/.env` | `APP_USER_PASSWORD`, `ORACLE_PASSWORD` | Paso 7 (documental; solo afecta a una base nueva o a la recreación del contenedor) |
| `backend/.env.example`, `infra/.env.example` | — | Sin cambios (siguen vacíos) |

Verificación, en este orden:

1. `sqlplus LAMB_APP@localhost:1521/FREEPDB1` con la clave nueva: conecta; con la antigua: `ORA-01017`.
2. Privilegios y objetos iguales a la foto del paso 2.
3. `cd backend; .\mvnw.cmd test -Dtest=LambAcademicApplicationTests` → 1/1 (conexión y validación del esquema).
4. `.\mvnw.cmd clean test` → 83/83 (incluye Oracle real y `ModularityTests`).
5. `.\mvnw.cmd spring-boot:run` y `http://localhost:8080/actuator/health` → `status: UP`, `db: UP`.
6. `python -I infra/pruebas/e2e_repetible.py` → 99/99 y conteos de tablas iguales al inicio.
7. Docker intacto: `docker inspect lamb-academic-oracle --format '{{.State.Status}} {{.RestartCount}}'`
   igual que antes y volumen `lamb-academic-dev_oracle-data` sin cambios.

## 8. Decisiones que requieren autorización

| # | Decisión | Recomendación |
|---|---|---|
| D1 | Ejecutar la rotación de `LAMB_APP` y de `SYS`/`SYSTEM`/`PDBADMIN` (sección 4) | **Sí**, antes de compartir el repositorio |
| D2 | Activar el hook en este clon (`git config core.hooksPath .githooks`) y pedirlo a los compañeros | **Sí** |
| D3 | Recrear el contenedor con `docker compose up -d` para que sus variables reflejen las claves nuevas y, en la misma operación, publicar Oracle solo en `127.0.0.1:1522` | Recomendado; conserva el volumen. Requiere respaldo previo |
| D4 | Sanear el historial y forzar el push | **No**, salvo que el repositorio vaya a ser público |
| D5 | Eliminar los archivos `verif_*.sql` del directorio Data Pump del contenedor y depurar el objeto local inalcanzable (`git gc --prune=now`) | Opcional; no son datos de la aplicación |
| D6 | Ignorar en `D:\LP2\.gitignore` el ZIP y la carpeta de insumos | **Sí** (evita publicarlos por accidente en `bomerp`) |
