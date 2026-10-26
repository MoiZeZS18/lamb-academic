import { Seccion } from '../seccion/seccion.model';
import { ESTADO_ANULADA, Matricula } from './matricula.model';

/**
 * Consistencia academica de una matricula frente a los datos reales de los otros modulos.
 *
 * Las matriculas registradas antes de la integracion (Unidad I) quedaron con referencias que hoy
 * no existen: alumno ausente en Base Universal, secciones ausentes en Core Academico, detalles sin
 * curso y sin creditos. El backend las sigue contando en sus agregados (son filas reales); la SPA
 * las separa para no presentar cifras enganosas. Nunca se modifican ni se eliminan desde aqui.
 *
 * Criterios (todos verificables con las APIs existentes):
 *  1. El alumno existe en Base Universal (solo si se conoce la lista de alumnos: personal).
 *  2. Tiene al menos un curso.
 *  3. Cada seccion existe y pertenece al periodo de la matricula.
 *  4. Cada detalle identifica su curso.
 *  5. Registra el total de creditos (lo calcula el backend al registrar desde la integracion).
 */
export interface ReferenciasMatricula {
  /** ALUMNOS.ID existentes; null si el usuario no puede consultarlos (ESTUDIANTE). */
  alumnos: ReadonlySet<number> | null;
  /** Secciones del periodo de la matricula (por id). */
  secciones: ReadonlyMap<number, Seccion>;
}

export function motivosRevision(matricula: Matricula, ref: ReferenciasMatricula): string[] {
  const motivos: string[] = [];
  if (ref.alumnos && !ref.alumnos.has(matricula.estudianteId)) {
    motivos.push(`El alumno ${matricula.estudianteId} no existe en Base Universal.`);
  }
  if (!matricula.detalles.length) {
    motivos.push('No tiene cursos registrados.');
  }
  const seccionesAusentes = matricula.detalles
    .filter((d) => ref.secciones.get(d.seccionId)?.periodoId !== matricula.periodoId)
    .map((d) => d.seccionId);
  if (seccionesAusentes.length) {
    motivos.push(
      `${seccionesAusentes.length === 1 ? 'La sección' : 'Las secciones'} ${seccionesAusentes.join(', ')} ` +
        `no ${seccionesAusentes.length === 1 ? 'existe' : 'existen'} en la oferta del periodo.`,
    );
  }
  const sinCurso = matricula.detalles.filter((d) => d.cursoId === null).length;
  if (sinCurso) {
    motivos.push(`${sinCurso} ${sinCurso === 1 ? 'curso no identificado' : 'cursos no identificados'}.`);
  }
  if (matricula.totalCreditos === null) {
    motivos.push('No registra el total de créditos.');
  }
  return motivos;
}

export interface ClasificacionPeriodo {
  /** Matriculas no anuladas (todas las que el backend cuenta como activas). */
  registradas: Matricula[];
  consistentes: Matricula[];
  revision: { matricula: Matricula; motivos: string[] }[];
  anuladas: Matricula[];
  inscripcionesConsistentes: number;
  inscripcionesRevision: number;
}

/** Clasifica las matriculas de un periodo. Las anuladas se apartan: no son activas ni se revisan. */
export function clasificarMatriculas(matriculas: Matricula[], ref: ReferenciasMatricula): ClasificacionPeriodo {
  const anuladas = matriculas.filter((m) => m.estado === ESTADO_ANULADA);
  const registradas = matriculas.filter((m) => m.estado !== ESTADO_ANULADA);
  const revision = registradas
    .map((matricula) => ({ matricula, motivos: motivosRevision(matricula, ref) }))
    .filter((r) => r.motivos.length > 0);
  const enRevision = new Set(revision.map((r) => r.matricula.id));
  const consistentes = registradas.filter((m) => !enRevision.has(m.id));
  const detalles = (lista: Matricula[]) => lista.reduce((t, m) => t + m.detalles.length, 0);
  return {
    registradas,
    consistentes,
    revision,
    anuladas,
    inscripcionesConsistentes: detalles(consistentes),
    inscripcionesRevision: detalles(revision.map((r) => r.matricula)),
  };
}
