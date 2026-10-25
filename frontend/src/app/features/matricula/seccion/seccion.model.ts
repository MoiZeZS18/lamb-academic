import { CursoResumen } from '../../core-academico/curso/curso.model';

/** SeccionResponse / SeccionRequest de Core Academico (coreacademico.seccion). */
export type DiaSemana = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES' | 'SABADO' | 'DOMINGO';
export type EstadoSeccion = 'ABIERTA' | 'CERRADA' | 'CANCELADA';

export const DIAS_SEMANA: DiaSemana[] = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
export const ESTADOS_SECCION: EstadoSeccion[] = ['ABIERTA', 'CERRADA', 'CANCELADA'];

export interface Horario {
  id: number;
  diaSemana: DiaSemana;
  /** HH:mm */
  horaInicio: string;
  horaFin: string;
  aula: string;
}

export interface HorarioRequest {
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  aula: string;
}

export interface Seccion {
  id: number;
  curso: CursoResumen;
  periodoId: number;
  grupo: string;
  docenteId: number | null;
  docenteNombre: string | null;
  limiteVacantes: number;
  cuposOcupados: number;
  vacantesDisponibles: number;
  estado: EstadoSeccion;
  horarios: Horario[];
}

export interface SeccionRequest {
  cursoId: number;
  periodoId: number;
  grupo: string;
  docenteId: number | null;
  limiteVacantes: number;
  horarios: HorarioRequest[];
}

/** El backend solo permite cambiar docente y vacantes de una seccion ya programada. */
export interface SeccionActualizacionRequest {
  docenteId: number | null;
  limiteVacantes: number;
}

export interface SeccionFiltros {
  periodoId?: number | null;
  cursoId?: number | null;
  docenteId?: number | null;
  estado?: EstadoSeccion | null;
  conVacantes?: boolean;
}

export interface SeccionOcupacion {
  cursoCodigo: string;
  cursoNombre: string;
  secciones: number;
  vacantesTotales: number;
  cuposOcupados: number;
}
