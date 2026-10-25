import { TestBed } from '@angular/core/testing';
import { BloqueSemanal, HorarioSemanal } from './horario-semanal';
import { aMinutos, buscarCruces, resumenHorario, seCruzan } from './horario-util';

describe('horario-util', () => {
  it('convierte HH:mm a minutos', () => {
    expect(aMinutos('08:30')).toBe(510);
  });

  it('detecta cruces con el mismo criterio del backend (rangos que se solapan el mismo dia)', () => {
    const lunes810 = { diaSemana: 'LUNES' as const, horaInicio: '08:00', horaFin: '10:00' };
    expect(seCruzan(lunes810, { diaSemana: 'LUNES', horaInicio: '09:00', horaFin: '11:00' })).toBe(true);
    expect(seCruzan(lunes810, { diaSemana: 'LUNES', horaInicio: '10:00', horaFin: '12:00' })).toBe(false);
    expect(seCruzan(lunes810, { diaSemana: 'MARTES', horaInicio: '08:00', horaFin: '10:00' })).toBe(false);
  });

  it('buscarCruces describe el cruce entre secciones distintas', () => {
    const cruces = buscarCruces([
      { clave: '1001', etiqueta: 'IS101-A', horarios: [{ diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00' }] },
      { clave: '1004', etiqueta: 'IS104-A', horarios: [{ diaSemana: 'LUNES', horaInicio: '09:00', horaFin: '11:00' }] },
      { clave: '1006', etiqueta: 'IS202-A', horarios: [{ diaSemana: 'MIERCOLES', horaInicio: '14:00', horaFin: '16:00' }] },
    ]);
    expect(cruces.length).toBe(1);
    expect(cruces[0].descripcion).toContain('IS101-A (Lunes 08:00-10:00) se cruza con IS104-A');
  });

  it('resume el horario en texto compacto', () => {
    expect(
      resumenHorario([
        { diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00' },
        { diaSemana: 'MIERCOLES', horaInicio: '08:00', horaFin: '10:00' },
      ]),
    ).toBe('Lun 08:00-10:00, Mié 08:00-10:00');
  });
});

describe('HorarioSemanal', () => {
  async function render(bloques: BloqueSemanal[]) {
    await TestBed.configureTestingModule({ imports: [HorarioSemanal] }).compileComponents();
    const fixture = TestBed.createComponent(HorarioSemanal);
    fixture.componentRef.setInput('bloques', bloques);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => TestBed.resetTestingModule());

  it('sin bloques no dibuja clases', async () => {
    const html = await render([]);
    expect(html.querySelectorAll('.bloque').length).toBe(0);
  });

  it('dibuja cada bloque en su dia y separa los que se solapan en carriles', async () => {
    const html = await render([
      { clave: '1', dia: 'LUNES', inicio: '08:00', fin: '10:00', titulo: 'IS101-A', conflicto: true },
      { clave: '2', dia: 'LUNES', inicio: '09:00', fin: '11:00', titulo: 'IS104-A', conflicto: true },
      { clave: '1', dia: 'MIERCOLES', inicio: '08:00', fin: '10:00', titulo: 'IS101-A' },
    ]);
    const bloques = [...html.querySelectorAll<HTMLElement>('.bloque')];
    expect(bloques.length).toBe(3);
    const [a, b] = bloques.filter((x) => x.textContent!.includes('IS10') && x.classList.contains('conflicto'));
    expect(a.style.width).toContain('50%');
    expect(b.style.left).toContain('50%');
    expect(bloques[2].classList.contains('conflicto')).toBe(false);
  });
});
