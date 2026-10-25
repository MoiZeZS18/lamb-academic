import { Periodo } from '../periodo/periodo.model';
import { ESTADO_ANULADA, Matricula } from './matricula.model';

/** Fecha local de hoy en ISO (yyyy-MM-dd), sin desfase de zona horaria. */
export function hoyIso(hoy = new Date()): string {
  const mes = String(hoy.getMonth() + 1).padStart(2, '0');
  const dia = String(hoy.getDate()).padStart(2, '0');
  return `${hoy.getFullYear()}-${mes}-${dia}`;
}

/** El periodo vigente es el ACTIVO (si hubiera varios, el de inicio mas reciente). */
export function periodoActivo(periodos: Periodo[]): Periodo | null {
  return (
    periodos
      .filter((p) => p.estado === 'ACTIVO')
      .sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio))[0] ?? null
  );
}

/**
 * Motivo por el que hoy NO se puede registrar una matricula en el periodo, o null si se puede.
 * Son las mismas reglas que MatriculaService.registrar valida (periodo ACTIVO y fecha dentro del
 * periodo); el backend sigue siendo la autoridad final.
 */
export function motivoBloqueo(periodo: Periodo | null, hoy = hoyIso()): string | null {
  if (!periodo) {
    return 'No hay un periodo académico activo. La matrícula se habilita cuando la administración activa un periodo.';
  }
  if (periodo.estado !== 'ACTIVO') {
    return `El periodo ${periodo.codigo} está ${periodo.estado.toLowerCase()} y no admite matrículas.`;
  }
  if (hoy < periodo.fechaInicio) {
    return `La matrícula del periodo ${periodo.codigo} se habilita desde su inicio (${formatoFecha(periodo.fechaInicio)}).`;
  }
  if (hoy > periodo.fechaFin) {
    return `El periodo ${periodo.codigo} terminó el ${formatoFecha(periodo.fechaFin)}.`;
  }
  return null;
}

/** Matricula vigente (no anulada) dentro de una lista; el backend permite una sola por periodo. */
export function matriculaVigente(matriculas: Matricula[]): Matricula | null {
  return matriculas.find((m) => m.estado !== ESTADO_ANULADA) ?? null;
}

export function formatoFecha(iso: string): string {
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}
