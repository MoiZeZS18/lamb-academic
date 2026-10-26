/** Contratos de /api/v1/matriculas (MatriculaRegistroRequest, MatriculaResponse, ...). */
export type ResultadoCurso = 'APROBADO' | 'DESAPROBADO' | 'RETIRADO';

/** Estado con el que la SPA registra (el mismo de las matriculas ya existentes). */
export const ESTADO_INICIAL_MATRICULA = 'INICIADA';
export const ESTADO_ANULADA = 'ANULADA';

export interface MatriculaRequest {
  estudianteId: number;
  periodoId: number;
  /** ISO yyyy-MM-dd; el backend exige que caiga dentro del periodo. */
  fecha: string;
  estado: string;
  numeroCuotas: number | null;
}

export interface DetalleMatriculaRequest {
  seccionId: number;
}

/** Una sola operacion transaccional: cabecera + detalles (S9). */
export interface MatriculaRegistroRequest {
  matricula: MatriculaRequest;
  detalles: DetalleMatriculaRequest[];
}

export interface DetalleMatricula {
  id: number;
  seccionId: number;
  /** null en las matriculas previas a la integracion. */
  cursoId: number | null;
  creditos: number | null;
  resultado: ResultadoCurso | null;
}

export interface CargoMatricula {
  cargoId: number;
  numeroCargo: string;
  montoTotal: number;
  numeroCuotas: number;
  estado: string;
}

export interface Matricula {
  id: number;
  estudianteId: number;
  periodoId: number;
  fecha: string;
  estado: string;
  detalles: DetalleMatricula[];
  totalCreditos: number | null;
  cargo: CargoMatricula | null;
}

export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface MatriculaFiltros {
  estudianteId?: number | null;
  periodoId?: number | null;
  estado?: string | null;
}

export interface MatriculaReporte {
  totalMatriculas: number;
  totalDetalles: number;
}

/** GET /api/v1/matriculas/contexto: escuela y habilitacion del alumno (Base Universal). */
export interface ContextoAcademico {
  estudianteId: number;
  existe: boolean;
  habilitado: boolean;
  escuelaId: number | null;
}
