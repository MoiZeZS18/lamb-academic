import { Matricula } from '../app/features/matricula/matricula/matricula.model';
import { Seccion } from '../app/features/matricula/seccion/seccion.model';

/**
 * Datos de prueba con la forma exacta de las respuestas del backend (PeriodoResponse,
 * SeccionResponse, MatriculaResponse). Solo viven en las pruebas: la SPA nunca los usa.
 */
export const API = 'http://localhost:8080/api/v1';

export const PERIODO_ACTIVO = {
  id: 4,
  codigo: '2026-II',
  nombre: 'Periodo 2026-II',
  fechaInicio: '2000-01-01',
  fechaFin: '2999-12-31',
  estado: 'ACTIVO',
};

export const PERIODO_CERRADO = {
  id: 24,
  codigo: '2026-I',
  nombre: 'Periodo 2026-I',
  fechaInicio: '1999-03-01',
  fechaFin: '1999-07-15',
  estado: 'CERRADO',
};

export function seccionDePrueba(id: number, codigo: string, extra: Partial<Seccion> = {}): Seccion {
  return {
    id,
    curso: { id, codigo, nombre: `Curso ${codigo}`, creditos: 4 },
    periodoId: 4,
    grupo: 'A',
    docenteId: 4,
    docenteNombre: 'Mendoza Ramos Carlos',
    limiteVacantes: 30,
    cuposOcupados: 1,
    vacantesDisponibles: 29,
    estado: 'ABIERTA',
    horarios: [{ id: id * 10, diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00', aula: 'A-101' }],
    ...extra,
  };
}

export function matriculaDePrueba(id: number, extra: Partial<Matricula> = {}): Matricula {
  return {
    id,
    estudianteId: 1001,
    periodoId: 4,
    fecha: '2026-10-24',
    estado: 'INICIADA',
    detalles: [{ id: id * 10, seccionId: 1001, cursoId: 1001, creditos: 4, resultado: null }],
    totalCreditos: 4,
    cargo: null,
    ...extra,
  };
}

export function pagina<T>(content: T[]) {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

export function detalleDePrueba(id: number, seccionId: number, cursoId: number | null = seccionId) {
  return { id, seccionId, cursoId, creditos: cursoId ? 4 : null, resultado: null };
}

/** Matricula previa a la integracion: alumno 1 inexistente, secciones inexistentes, sin creditos. */
export function matriculaHistorica(id: number, secciones: number[]): Matricula {
  return matriculaDePrueba(id, {
    estudianteId: 1,
    totalCreditos: null,
    detalles: secciones.map((s, i) => detalleDePrueba(id * 10 + i, s, null)),
  });
}

/**
 * Forma real del periodo 2026-II en Oracle (verificada con consultas de solo lectura): 7 matriculas
 * historicas (alumno 1, secciones 1, 2, 100 y 101) y 2 consistentes (93 y 94) con 5 inscripciones;
 * se agrega una anulada (95) para probar que no cuenta como activa.
 */
export const ALUMNOS_IDS = [1001, 1002, 1003, 1033];
export const SECCIONES_2026_II = [1001, 1002, 1003, 1005].map((id) => seccionDePrueba(id, `IS${id}`));
export const MATRICULAS_2026_II: Matricula[] = [
  matriculaHistorica(1, [1, 2]),
  matriculaHistorica(25, [100]),
  matriculaHistorica(26, [100, 101]),
  matriculaHistorica(27, [100]),
  matriculaHistorica(28, [100]),
  matriculaHistorica(29, [100]),
  matriculaHistorica(41, [100]),
  matriculaDePrueba(93, {
    detalles: [detalleDePrueba(1, 1001), detalleDePrueba(2, 1002), detalleDePrueba(3, 1003)],
    totalCreditos: 11,
  }),
  matriculaDePrueba(94, { estudianteId: 1002, detalles: [detalleDePrueba(4, 1003), detalleDePrueba(5, 1005)], totalCreditos: 7 }),
  matriculaDePrueba(95, { estudianteId: 1033, estado: 'ANULADA', detalles: [detalleDePrueba(6, 1001)] }),
];

/** Respuesta minima de /alumnos con los ids dados (solo se usa el id para la consistencia). */
export function alumnosDePrueba(ids: number[]) {
  return ids.map((id) => ({
    id,
    codigoEstudiante: `2026${id}`,
    estado: 'ACTIVO',
    fechaIngreso: '2026-03-01',
    habilitado: true,
    persona: { id, numeroDocumento: `7000${id}`, nombreCompleto: `Alumno ${id}`, email: null, idEscuela: 1, escuelaNombre: 'EPIS' },
  }));
}
