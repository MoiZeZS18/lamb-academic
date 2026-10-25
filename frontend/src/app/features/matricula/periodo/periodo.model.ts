/** PeriodoResponse / PeriodoRequest del backend (matricula.periodo). */
export type EstadoPeriodo = 'PLANIFICADO' | 'ACTIVO' | 'CERRADO';

export const ESTADOS_PERIODO: EstadoPeriodo[] = ['PLANIFICADO', 'ACTIVO', 'CERRADO'];

export interface Periodo {
  id: number;
  codigo: string;
  nombre: string;
  /** ISO yyyy-MM-dd */
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoPeriodo;
}

export interface PeriodoRequest {
  codigo: string;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoPeriodo;
}
