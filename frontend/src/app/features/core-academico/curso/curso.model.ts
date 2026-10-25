/** CursoResponse / CursoResumen del backend (coreacademico.curso). Solo lectura desde Matricula. */
export interface Curso {
  id: number;
  codigo: string;
  nombre: string;
  creditos: number;
  horasTeoricas: number | null;
  horasPracticas: number | null;
  activo: boolean;
}

export interface CursoResumen {
  id: number;
  codigo: string;
  nombre: string;
  creditos: number;
}
