/** PlanEstudioResponse de Core Academico (coreacademico.plan). Solo lectura desde Matricula. */
export type TipoCursoPlan = 'OBLIGATORIO' | 'ELECTIVO';

export interface DetallePlan {
  id: number;
  idCurso: number;
  cursoCodigo: string;
  cursoNombre: string;
  ciclo: number;
  creditos: number;
  tipo: TipoCursoPlan;
  idPrerrequisito: number | null;
  prerrequisitoCodigo: string | null;
}

export interface PlanEstudio {
  id: number;
  idEscuela: number;
  escuelaNombre: string | null;
  codigo: string;
  nombre: string;
  anio: number;
  version: number;
  estado: 'VIGENTE' | 'OBSOLETO';
  totalCreditos: number;
  totalCursos: number;
  detalles: DetallePlan[];
}
