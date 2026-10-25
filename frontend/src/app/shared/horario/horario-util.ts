import { DiaSemana, Horario, HorarioRequest } from '../../features/matricula/seccion/seccion.model';

type BloqueHorario = Pick<Horario | HorarioRequest, 'diaSemana' | 'horaInicio' | 'horaFin'>;

export const NOMBRE_DIA: Record<DiaSemana, string> = {
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miércoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sábado',
  DOMINGO: 'Domingo',
};

export const DIA_CORTO: Record<DiaSemana, string> = {
  LUNES: 'Lun',
  MARTES: 'Mar',
  MIERCOLES: 'Mié',
  JUEVES: 'Jue',
  VIERNES: 'Vie',
  SABADO: 'Sáb',
  DOMINGO: 'Dom',
};

/** Franja institucional del backend (SeccionService.HORA_MINIMA / HORA_MAXIMA). */
export const HORA_MINIMA = 7;
export const HORA_MAXIMA = 22;

/** "08:30" -> 510 minutos desde las 00:00. */
export function aMinutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + (m || 0);
}

/** Mismo criterio que MatriculaService.existeCruce: mismo dia y rangos que se solapan. */
export function seCruzan(a: BloqueHorario, b: BloqueHorario): boolean {
  return (
    a.diaSemana === b.diaSemana &&
    aMinutos(a.horaInicio) < aMinutos(b.horaFin) &&
    aMinutos(b.horaInicio) < aMinutos(a.horaFin)
  );
}

export interface ElementoConHorario {
  clave: string;
  etiqueta: string;
  horarios: BloqueHorario[];
}

export interface CruceDetectado {
  a: string;
  b: string;
  dia: DiaSemana;
  descripcion: string;
}

/** Todos los cruces entre elementos distintos (p. ej. secciones elegidas en la matricula). */
export function buscarCruces(elementos: ElementoConHorario[]): CruceDetectado[] {
  const cruces: CruceDetectado[] = [];
  for (let i = 0; i < elementos.length; i++) {
    for (let j = i + 1; j < elementos.length; j++) {
      for (const ha of elementos[i].horarios) {
        const hb = elementos[j].horarios.find((h) => seCruzan(ha, h));
        if (hb) {
          cruces.push({
            a: elementos[i].clave,
            b: elementos[j].clave,
            dia: ha.diaSemana,
            descripcion:
              `${elementos[i].etiqueta} (${NOMBRE_DIA[ha.diaSemana]} ${ha.horaInicio}-${ha.horaFin}) se cruza con ` +
              `${elementos[j].etiqueta} (${hb.horaInicio}-${hb.horaFin})`,
          });
          break;
        }
      }
    }
  }
  return cruces;
}

/** Texto compacto: "Lun 08:00-10:00, Mié 08:00-10:00". */
export function resumenHorario(horarios: BloqueHorario[]): string {
  return horarios.map((h) => `${DIA_CORTO[h.diaSemana]} ${h.horaInicio}-${h.horaFin}`).join(', ');
}
