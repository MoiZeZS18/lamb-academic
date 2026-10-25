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
