import {
  ALUMNOS_IDS,
  MATRICULAS_2026_II,
  SECCIONES_2026_II,
  matriculaDePrueba,
  matriculaHistorica,
  seccionDePrueba,
} from '../../../../testing/datos-matricula';
import { clasificarMatriculas, motivosRevision } from './consistencia-matricula';

const REF = {
  alumnos: new Set(ALUMNOS_IDS),
  secciones: new Map(SECCIONES_2026_II.map((s) => [s.id, s] as const)),
};

describe('consistencia de matriculas (registros historicos)', () => {
  it('reproduce 2026-II: 2 consistentes con 5 inscripciones y 7 registros en revision', () => {
    const c = clasificarMatriculas(MATRICULAS_2026_II, REF);
    expect(c.registradas.length).toBe(9);
    expect(c.consistentes.map((m) => m.id)).toEqual([93, 94]);
    expect(c.inscripcionesConsistentes).toBe(5);
    expect(c.revision.map((r) => r.matricula.id)).toEqual([1, 25, 26, 27, 28, 29, 41]);
    expect(c.inscripcionesRevision).toBe(9);
    expect(c.anuladas.map((m) => m.id)).toEqual([95]);
    // Trazabilidad: consistentes + revision = total registrado por el servidor.
    expect(c.consistentes.length + c.revision.length).toBe(c.registradas.length);
  });

  it('explica cada motivo, no solo la falta de creditos', () => {
    expect(motivosRevision(matriculaHistorica(26, [100, 101]), REF)).toEqual([
      'El alumno 1 no existe en Base Universal.',
      'Las secciones 100, 101 no existen en la oferta del periodo.',
      '2 cursos no identificados.',
      'No registra el total de créditos.',
    ]);
  });

  it('una matricula con creditos pero con una seccion de otro periodo tambien se revisa', () => {
    const otraOferta = new Map([[1001, seccionDePrueba(1001, 'IS101', { periodoId: 24 })]]);
    const m = matriculaDePrueba(97, { totalCreditos: 4 });
    expect(motivosRevision(m, { alumnos: REF.alumnos, secciones: otraOferta })).toEqual([
      'La sección 1001 no existe en la oferta del periodo.',
    ]);
  });

  it('sin lista de alumnos (ESTUDIANTE) no se juzga al alumno, solo los datos academicos', () => {
    expect(motivosRevision(matriculaDePrueba(93), { alumnos: null, secciones: REF.secciones })).toEqual([]);
  });

  it('una matricula sin cursos requiere revision', () => {
    expect(motivosRevision(matriculaDePrueba(98, { detalles: [] }), REF)).toEqual(['No tiene cursos registrados.']);
  });
});
