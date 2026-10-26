# Guía breve de demostración · Módulo Matrícula

Todas las operaciones de esta guía se comprobaron el 2026-10-26 contra el backend real, Keycloak
(puerto 48080) y Oracle (`lamb-academic-oracle`). No hay datos simulados: lo que se ve son los
registros del periodo 2026-II.

## 1. Antes de la clase (5 minutos)

```powershell
docker ps                     # lamb-academic-oracle, lamb-academic-keycloak, lamb-academic-keycloak-db
cd D:\LP2\lamb-academic\backend; .\mvnw.cmd spring-boot:run      # http://localhost:8080/actuator/health = UP
cd D:\LP2\lamb-academic\frontend; npm.cmd start                  # http://localhost:4200
python -I infra/pruebas/verificar_keycloak.py                    # 8/8: login PKCE y roles de las 5 cuentas
```

- Abrir una ventana de incógnito en `http://localhost:4200`.
- Las claves de demostración están en `infra/secretos/keycloak-usuarios.env` (no proyectar ese
  archivo). Para generarlas o revertirlas: `docs/SEGURIDAD_KEYCLOAK.md` §8.

## 2. Inicio de sesión institucional

“Iniciar sesión” lleva al login de Keycloak con la identidad UPeU (logo oficial, LAMB Academic).
Es Authorization Code + PKCE: la SPA nunca ve la contraseña. Una clave equivocada muestra
“Usuario o contraseña incorrectos.” en el mismo formulario.

## 3. Recorrido como administrador (`admin.lamb`)

| Paso | Dónde | Qué mostrar |
|---|---|---|
| 1 | Portal de matrícula | Periodo vigente 2026-II, inscripción habilitada, 6 secciones abiertas y 142 vacantes. Indicadores: **2 matrículas activas consistentes, 5 inscripciones**; aviso de 7 registros históricos en revisión |
| 2 | Periodos | 2026-II activo y 2026-I cerrado; crear/editar/eliminar solo para ADMIN; al editar 2026-I avisa que está cerrado (cancelar sin guardar) |
| 3 | Secciones | Curso, docente, horario, “Ocupados / capacidad” y “Disponibles” (IS103-A: 2 / 2, sin vacantes); eliminar deshabilitado con el motivo cuando hay matriculados |
| 4 | Inscripción | Elegir alumno (buscar “apaza”): aparece su plan PLAN-EPIS-2026, cada curso con su ciclo y prerrequisito (IS201 requiere IS101), filtro por ciclo, créditos y cruces en vivo. **No registrar** si no se quiere crear datos |
| 5 | Matrículas | Filtros por periodo, estado y alumno; los 7 registros históricos llevan la marca “Requiere revisión” |
| 6 | Detalle de la matrícula 93 | Cursos, créditos, cargo y cuotas; en la matrícula 1 se explica por qué es un registro histórico |
| 7 | Horario | Buscar por periodo y alumno (Quispe): calendario semanal con colores por curso |
| 8 | Reportes | Conciliación: 9 registradas = 2 consistentes + 7 en revisión; 5 cupos ocupados = 5 inscripciones consistentes; cargos vigentes, pagos, saldo y anulados por separado |

## 4. Recorrido como estudiante (`ana.quispe`)

| Paso | Dónde | Qué mostrar |
|---|---|---|
| 1 | Menú | Solo Portal, Inscripción, **Mis matrículas** y **Mi horario** (sin gestión académica) |
| 2 | Portal | Matrícula 93: 3 cursos y 11 créditos, ciclo de cada curso en su plan, resultado “En curso”, escuela y plan; cargo con monto, pagado, saldo y próximo vencimiento |
| 3 | Mi horario | Su semana sin buscar nada (el backend usa el alumno del token) |
| 4 | Inscripción | “Ya tienes la matrícula 93 (INICIADA) en este periodo”: no puede duplicarla |
| 5 | 403 provocado | Escribir `http://localhost:4200/matricula/consulta/94` → “No tienes acceso a la matrícula de otro estudiante” (lo decide el backend) |
| 6 | Ruta no permitida | `http://localhost:4200/matricula/reportes` → “Acceso no permitido” |

## 5. Qué es propio de Matrícula y qué depende de otros módulos

| Implementado en Matrícula | Depende de otro módulo (solo se consulta) |
|---|---|
| Periodos (CRUD), inscripción cabecera-detalle, consulta, detalle, anulación, horario, reportes de matrícula, revisión de registros históricos | Alumnos y su escuela (Base Universal), cursos, planes y prerrequisitos (Core Académico), cargos, cuotas y pagos (Finanzas) |
| `GET /api/v1/matriculas/contexto` (escuela y habilitación del alumno, para la inscripción) | CRUD de cursos y mallas: Core Académico. Registro de pagos: caja (Finanzas) |
| Secciones: pantalla usada como CRUD dependiente (S8) | El backend de secciones es de Core Académico; su compañero tiene otra pantalla que debe acordarse antes de integrar |

## 6. Después de la presentación

Volver a claves largas aleatorias (`docs/SEGURIDAD_KEYCLOAK.md` §8) y comprobar con
`verificar_keycloak.py`.
