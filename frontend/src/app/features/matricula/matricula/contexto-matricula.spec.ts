import { Periodo } from '../periodo/periodo.model';
import { hoyIso, matriculaVigente, motivoBloqueo, periodoActivo } from './contexto-matricula';
import { Matricula } from './matricula.model';

const p = (estado: Periodo['estado'], fechaInicio = '2026-08-01', fechaFin = '2026-12-20'): Periodo => ({
  id: 4,
  codigo: '2026-II',
  nombre: 'Periodo 2026-II',
  fechaInicio,
  fechaFin,
  estado,
});

describe('contexto de matricula', () => {
  it('hoyIso usa la fecha local', () => {
    expect(hoyIso(new Date(2026, 9, 24, 23, 30))).toBe('2026-10-24');
  });

  it('periodoActivo elige el ACTIVO', () => {
    expect(periodoActivo([p('CERRADO'), { ...p('ACTIVO'), id: 9 }])?.id).toBe(9);
    expect(periodoActivo([p('PLANIFICADO')])).toBeNull();
  });

  it('motivoBloqueo explica por que no se puede matricular, con las fechas reales del periodo', () => {
    expect(motivoBloqueo(null)).toContain('No hay un periodo académico activo');
    expect(motivoBloqueo(p('CERRADO'))).toContain('cerrado');
    expect(motivoBloqueo(p('ACTIVO'), '2026-07-31')).toContain('01/08/2026');
    expect(motivoBloqueo(p('ACTIVO'), '2026-12-21')).toContain('20/12/2026');
    expect(motivoBloqueo(p('ACTIVO'), '2026-10-24')).toBeNull();
  });

  it('matriculaVigente ignora las anuladas', () => {
    const m = (id: number, estado: string) => ({ id, estado }) as Matricula;
    expect(matriculaVigente([m(1, 'ANULADA'), m(2, 'INICIADA')])?.id).toBe(2);
    expect(matriculaVigente([m(1, 'ANULADA')])).toBeNull();
  });
});
