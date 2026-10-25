# Solicitud de revisión: `DELETE /api/v1/secciones/{id}` (Core Académico)

**Para:** responsable del módulo Core Académico (aporte original `lambacademic`: Curso, Sección, HorarioAula).
**Rama:** `unidad2/frontend-matricula` · **commit:** `5b0d3c5` · **estado:** implementado y probado; pendiente de tu aprobación antes de fusionar con `main`.

## 1. Por qué se agregó

La guía S8 exige un CRUD **completo** (listar, crear, editar y **eliminar**) de una tabla dependiente.
Sección depende de Curso y de Periodo, pero el contrato original solo ofrecía `GET`, `POST`, `PUT` y
`PATCH /estado`: no había forma de eliminar. Es el cambio mínimo para cumplir S8 sin alterar ninguna
operación existente.

## 2. Contrato nuevo

| Método y ruta | Respuesta | Errores |
|---|---|---|
| `DELETE /api/v1/secciones/{id}` | `204 No Content` | `404` si la sección no existe · `409` si ya se usó o su periodo está cerrado · `403` si el rol no es COORDINADOR ni ADMIN · `401` sin sesión |

Mensajes del `409` (los muestra la SPA tal cual):

- `No se puede eliminar la seccion {id}: ya tiene matriculas registradas. Cancelala en su lugar`
- `El periodo {codigo} esta cerrado`

Ninguna operación existente cambió: `GET`, `POST`, `PUT /{id}`, `PATCH /{id}/estado` y `GET /resumen`
mantienen su contrato y su comportamiento.

## 3. Reglas de negocio (`SeccionService.eliminar`)

Solo se elimina una sección **que nunca se usó**:

1. Debe existir (si no, `404`, igual que `obtener`).
2. `cuposOcupados` debe ser 0.
3. No debe aparecer en ningún detalle de matrícula, **ni siquiera de una matrícula anulada**: el
   detalle anulado conserva la historia académica y depende de esa sección.
4. Su periodo no debe estar cerrado (se reutiliza `validarPeriodo`, la misma regla de `programar`).

Si ya se usó, la operación correcta sigue siendo **cancelarla** con `PATCH /{id}/estado` (regla que
ya existía). Los horarios se eliminan junto con la sección por la relación existente
`@OneToMany(cascade = ALL, orphanRemoval = true)` de `Seccion.horarios`.

## 4. Diseño: puerto `SeccionEnUsoPort` (sin ciclo entre módulos)

La regla 3 necesita consultar `DETALLE_MATRICULA`, que pertenece al módulo **Matrícula**. Matrícula ya
depende de Core Académico, así que Core no puede depender de Matrícula sin crear un ciclo. Se aplicó el
**mismo patrón que ya usa `PeriodoAcademicoPort`**:

```
coreacademico.seccion.port.SeccionEnUsoPort        (interfaz declarada por Core Académico)
        ▲ implementa
matricula.matricula.adapter.SeccionEnUsoAdapter    (Matrícula responde con DetalleMatriculaRepository.existsBySeccionId)
```

Además, en Oracle `DETALLE_MATRICULA` **no tiene FK** hacia `SECCIONES` (las matrículas previas a la
integración apuntan a secciones inexistentes), así que la verificación no se podía delegar a la base.
`ModularityTests` sigue en verde (3/3): no hay ciclos ni accesos a paquetes internos.

## 5. Archivos modificados

| Archivo | Cambio |
|---|---|
| `coreacademico/seccion/port/SeccionEnUsoPort.java` | **Nuevo**: `boolean tieneMatriculas(Long seccionId)` |
| `coreacademico/seccion/port/package-info.java` | Comentario: el `@NamedInterface("seccion-port")` incluye el nuevo puerto |
| `coreacademico/seccion/service/SeccionService.java` | Nueva dependencia `SeccionEnUsoPort` y método `eliminar(Long id)` |
| `coreacademico/seccion/controller/SeccionController.java` | `@DeleteMapping("/{id}")` con `204` |
| `matricula/matricula/adapter/SeccionEnUsoAdapter.java` | **Nuevo**: implementación del puerto |
| `matricula/matricula/repository/DetalleMatriculaRepository.java` | `boolean existsBySeccionId(Long seccionId)` (consulta derivada de Spring Data) |
| `SecurityConfig.java` | `DELETE /api/v1/secciones/**` solo para COORDINADOR y ADMIN |
| `test/.../SeccionServiceTest.java` | 4 pruebas nuevas y el mock del puerto |

No hay cambios de esquema en Oracle ni migraciones.

## 6. Pruebas que lo cubren

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `SeccionServiceTest.eliminarSeccionSinUso` | Sin cupos ni matrículas, periodo activo → `delete` | OK |
| `SeccionServiceTest.eliminarSeccionUsadaEnMatricula` | Aparece en una matrícula → 409, no borra | OK |
| `SeccionServiceTest.eliminarSeccionConCupos` | Cupos ocupados → 409, no borra | OK |
| `SeccionServiceTest.eliminarSeccionDePeriodoCerrado` | Periodo cerrado → 409, no borra | OK |
| `SecurityConfigTest.eliminarSeccionExigeCoordinacion` | ESTUDIANTE y CAJERO → 403; COORDINADOR → 204 | OK |
| `matriz_accesos.py` (tokens reales) | ESTUDIANTE y CAJERO → 403 sobre la sección 1004 | OK |
| Navegador (coordinación) | Eliminar IS101-A → 409 “ya tiene matriculas registradas”; crear y eliminar IS202-B → 204 | OK |
| `ModularityTests` | Límites de módulos sin ciclos | 3/3 |

## 7. Puntos que te pido revisar

1. ¿Estás de acuerdo con que una sección usada en una matrícula **anulada** tampoco se pueda borrar?
   (Se eligió conservar la historia; la alternativa sería permitirlo y dejar el detalle huérfano).
2. ¿Prefieres que la eliminación sea lógica (estado `CANCELADA`) en vez de física para secciones nunca
   usadas? Hoy la cancelación ya cubre el caso “usada”; la eliminación física solo aplica a errores de
   programación (por ejemplo, una sección creada por equivocación).
3. Los mensajes del `409` siguen el estilo actual del módulo (sin tildes); si el módulo adopta tildes,
   conviene cambiarlos junto con los demás.

## 8. Cómo revertir si no se aprueba

Revertir solo estos archivos del commit `5b0d3c5` (los cambios de seguridad del mismo commit son
independientes) y quitar en la SPA el botón “Eliminar” de `seccion-list.html` y el método `eliminar`
de `seccion-service.ts`. En ese caso el CRUD de S8 quedaría con baja lógica (cancelar) en lugar de
eliminar.
