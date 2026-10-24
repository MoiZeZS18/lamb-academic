# Guía técnica por módulo — para la revisión de cada integrante

Cada sección indica de qué aporte proviene el código, qué se conservó, qué cambió y por qué,
dónde está cada pieza y cómo comprobarla en vivo. Rutas relativas a
`backend/src/main/java/pe/edu/upeu/lambacademic/`. Pruebas en `backend/src/test/java/...`.

Patrón común (guías S1–S4): `controller → service → repository → entity`, DTO de entrada
(`*Request`, con Bean Validation) distinto del de salida (`*Response`), mapper MapStruct, excepciones
`ResourceNotFoundException` (404) y `ReglaNegocioException` (409) del paquete abierto `exception/`.
Los módulos solo se comunican por servicios marcados con `@NamedInterface` (`package-info.java`);
`ModularityTests` falla si alguien importa un repositorio o entidad de otro módulo.

## Evidencia de integración de los cinco integrantes

| Integrante (aporte) | Módulo / paquete | Evidencia automática | Evidencia HTTP (`e2e_repetible.py`) |
|---|---|---|---|
| Matrícula (proyecto principal) | `matricula.matricula`, `matricula.periodo` | `MatriculaServiceTest` (23), `MatriculaControllerTest` (5), `MatriculaOracleIntegrationTest` (6), `MatriculaRollbackOracleIntegrationTest` (2) | grupo «Matricula»: registro, duplicada, cruce, cupos, concurrencia, anulación, resultados |
| Base Universal (LP2_L) | `baseuniversal.persona`, `.alumno`, `.expediente` | `PersonaControllerTest` (5), integración (crea personas/alumnos reales) | grupo «Base Universal»: personas, alumnos, expediente con rollback |
| Finanzas (Frinanzas-lamb) | `finanzas.concepto`, `.cargo` | `CalculadoraCargoTest` (10), `CargoServiceTest` (8), integración (cargo con cuotas exactas) | grupo «Finanzas»: cuotas, pagos, saldo, idempotencia |
| Core Oferta (lambacademic) | `coreacademico.curso`, `.seccion` | `SeccionServiceTest` (10), integración (reserva atómica, concurrencia) | grupo «Core Academico»: secciones, cruces de aula/docente, carga |
| Core Currículo (LP2_G1) | `baseuniversal.institucional`, `coreacademico.plan`, `.docente` | `PlanEstudioServiceTest` (8), integración (malla con prerrequisito) | malla, prerrequisitos, distribución, árbol institucional |

---

## 1. Matrícula (trabajo original del proyecto principal)

**Conservado sin cambios:** paquetes `matricula.matricula` y `matricula.periodo`; las entidades
`Matricula`, `DetalleMatricula`, `Periodo` (con sus columnas originales); los puertos
`EstudianteConsultaPort` y `SeccionConsultaPort`; las tres consultas originales del repositorio
(`buscar`, `buscarResumen`, `generarReporte`); los endpoints `POST/GET /api/v1/matriculas`,
`/resumen`, `/reporte` y el CRUD de `/api/v1/periodos`; el orden de validación del registro
(periodo → activo → estudiante → habilitado → sección repetida → cupo → cruce de horarios) y el
algoritmo `existeCruce`.

**Agregado (marcado `[integracion]` en `MatriculaService`):** fecha dentro del periodo; una matrícula
activa por estudiante y periodo (`CLAVE_ACTIVA` + `UK_MATRICULA_ACTIVA`); sección del periodo y
ABIERTA; curso no repetido; prerrequisitos con resultado APROBADO; reserva atómica de cupos en orden
ascendente; cargo financiero en la misma transacción; `anular`, `generarCargo` (recuperación) y
`registrarResultado` (A1).

**Cambiado y por qué:**

| Cambio | Motivo |
|---|---|
| `System.out.println` → `log.debug` | Trazabilidad con `traceId` (S2 3.2.2) |
| Sección inexistente: `IllegalArgumentException` (500) → `SeccionNoEncontradaException` (404) | Contrato de errores S1 3.8 |
| Excepciones propias heredan de `ResourceNotFoundException`/`ReglaNegocioException`; se retiró el `GlobalExceptionHandler` local | Un solo manejador global (S2 3.2.1); había conflicto de beans |
| Un `saveAndFlush` con todos los detalles (antes uno por detalle) | Mismo resultado, menos viajes a Oracle |
| El controller ya no usa el repositorio; la búsqueda pasó al service con lista blanca de `sortBy` | «El controller nunca accede directo al repository» (S1 3.9); `sortBy` inválido daba 500 |
| `buscar` sin `@EntityGraph` + `@BatchSize(50)` en `detalles` | Con `JOIN FETCH` + `Pageable` Hibernate paginaba en memoria (`HHH90003004`) |
| `PeriodoService`: 404, `@Transactional`, fechas y código validados, no borrar con matrículas | Antes devolvía 500 y permitía dejar matrículas huérfanas |

**Adaptadores reales:** `EstudianteConsultaAdapter` → `AlumnoService` (Base Universal);
`SeccionConsultaAdapter` → `SeccionService` (Core); `PrerrequisitoConsultaAdapter` → `PlanEstudioService`;
`CargoFinancieroAdapter` → `CargoService` (Finanzas). Además Matrícula **implementa** los puertos que
Core y Finanzas declaran para consultar el periodo (`periodo/adapter/*`), evitando el ciclo de módulos.

**Para sustentar (S6 P2):** la línea que hace posible el rollback completo es `@Transactional` sobre
`MatriculaService.registrar`; las llamadas a Core y Finanzas usan propagación `REQUIRED` y se unen a
esa transacción. Prueba: `MatriculaRollbackOracleIntegrationTest`.

## 2. Base Universal — Persona, Alumno, Expediente, DocumentoAdjunto (aporte LP2_L)

**Origen:** `com.lamb.upeu.backend.baseuniversal` (Boot 3.2, Java 17). No compilaba
(`ExpedienteController` llamaba a métodos inexistentes), exponía entidades y usaba `ddl-auto: update`.

**Resultado:** `baseuniversal.persona` (modelo de Persona de LP2_G1, superconjunto del de LP2_L),
`baseuniversal.alumno` (`ALUMNOS`, 1:1 con `PERSONA`, estado `ACTIVO/SUSPENDIDO/RETIRADO/EGRESADO`
con transiciones válidas), `baseuniversal.expediente` (`EXPEDIENTES` → `DOCUMENTOS_ADJUNTOS`).

| Regla | Dónde |
|---|---|
| Solo una persona `ALUMNO` activa puede ser alumno; una persona, un alumno; código único | `AlumnoService.crear` |
| Una persona registrada como alumno no puede cambiar de tipo | `PersonaService.actualizar` |
| Expediente atómico: código único, 1–20 documentos, extensión = tipo, archivo no repetido | `ExpedienteService.registrar` |
| Flujo de estados del expediente | `ExpedienteService.cambiarEstado` |

**API pública:** `AlumnoService.buscarEstudiante(id) → EstudianteResumen` (lo usan Matrícula y Finanzas).
**Pospuesto:** JWT/`SecurityConfig` del aporte → U2 (S10).

## 3. Core Académico — Oferta y programación (aporte lambacademic)

**Origen:** `pe.edu.upeu.lambacademic_backend.coreacademico` (Boot 3.4). Funcional pero mínimo:
periodo como texto, sin cupos ocupados ni cruces.

**Resultado:** `coreacademico.curso` (catálogo único: unifica el Curso de lambacademic y el de LP2_G1)
y `coreacademico.seccion` (`SECCIONES` → `HORARIOS_AULA`). La programación de una sección con sus
horarios es una operación cabecera-detalle (`SeccionService.programar`).

| Regla | Dónde |
|---|---|
| Curso activo, periodo existente y no cerrado, docente válido, grupo único por curso y periodo | `programar` |
| Franja 07:00–22:00; inicio < fin; sin cruces internos, de aula ni de docente en el periodo | `validarHorario`, `cruceDeAula`, `cruceDeDocente` |
| Límite ≥ cupos ocupados; cancelar solo sin matriculados | `actualizar`, `cambiarEstado` |
| **Reserva atómica**: `UPDATE … WHERE CUPOS_OCUPADOS < LIMITE_VACANTES` (nativo) | `SeccionRepository.reservarCupo` |

`CUPOS_OCUPADOS` es `updatable = false` en la entidad: solo lo cambian las sentencias atómicas.
**API pública:** `obtenerParaMatricula`, `reservarCupo`, `liberarCupo`; puerto `PeriodoAcademicoPort`.

## 4. Core Académico — Currículo, institucional y plana docente (aporte LP2_G1)

**Origen:** `pe.edu.upeu.bomerp.base` (Boot 4.0.7), el aporte de mayor calidad; se integró casi tal cual.

**Resultado:** `baseuniversal.institucional` (Sede → Facultad → Escuela, árbol en `/institucional/arbol`),
`coreacademico.plan` (`PLAN_ESTUDIO` → `DETALLE_PLAN_ESTUDIO`, prerrequisito = `ID_PRERREQUISITO`),
`coreacademico.docente` (docente = Persona `DOCENTE`; carga lectiva desde las secciones).

| Cambio respecto al original | Motivo |
|---|---|
| `PlanEstudio.escuela` (`@ManyToOne`) → `escuelaId` validado con `InstitucionalService` | La escuela pertenece a otro módulo (Base Universal) |
| Prerrequisito del mismo plan y de un ciclo anterior (antes no se validaba) | Coherencia de la malla |
| Persona/Specifications → consulta `(:p IS NULL OR …)` | Patrón pedido en S5 / S6 P4 |
| `Boolean` con `BooleanNumeroConverter` | El original no arrancaba con `validate` en Oracle 23ai (esperaba `BOOLEAN`) |
| `resumen` devuelve listas `{categoria, cantidad}` | Contrato tipado (ver `CONTRATOS_REST.md`) |

Reglas originales conservadas: curso no repetido (regla 1), 6–24 créditos por ciclo (regla 2),
electivo no es prerrequisito (regla 3), plan OBSOLETO no se versiona (regla 4).
**API pública:** `PlanEstudioService.prerrequisitosDeCurso`, `InstitucionalService.obtenerEscuela`,
`PersonaService.obtenerDocente/listarDocentes`.

## 5. Finanzas del Estudiante (aporte Frinanzas-lamb)

**Origen:** `pe.edu.upeu.finanzas_backend.cargo` (Boot 3.4).

**Resultado:** `finanzas.concepto` (`FIN_CONCEPTOS_PAGO`: las tarifas 150.00 y 50.00/crédito del original
pasan a datos) y `finanzas.cargo` (`FIN_CARGOS` → `FIN_DETALLE_CARGOS`, `FIN_PAGOS`).

| Corrección | Detalle |
|---|---|
| Redondeo | Las cuotas sumaban distinto del total (1150/3 → 1149.99). Ahora la última cuota absorbe la diferencia (`CalculadoraCargo.fraccionar`) |
| Deuda | `deudaTotal` incluía cargos anulados |
| Duplicados | Índice único funcional `UX_FIN_CARGOS_ORIGEN` + `generarCargoMatricula` idempotente; cargo manual de MATRICULA rechazado |
| Concurrencia | `@Version` en `FIN_CARGOS` para pagos simultáneos |
| Pagos | Nuevos: se aplican a las cuotas en orden; el cargo con pagos no se anula |

**API pública:** `CargoService.generarCargoMatricula`, `anularCargoDeMatricula`; puerto `PeriodoFinancieroPort`.

## Cómo revisar cada módulo

1. Leer su sección aquí y su paquete en el código.
2. `cd backend && .\mvnw.cmd test -Dtest=<ClaseDePrueba>` (ver la tabla inicial).
3. Con el backend levantado, probar sus endpoints en `/swagger-ui.html`.
4. `.\mvnw.cmd test -Dtest=ModularityTests` para confirmar que el módulo respeta los límites.
