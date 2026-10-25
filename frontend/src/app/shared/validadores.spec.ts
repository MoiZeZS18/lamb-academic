import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { alMenosUnaLinea, entero, rangoFechas, sinRepetidos } from './validadores';

describe('validadores', () => {
  it('entero', () => {
    expect(entero(new FormControl(3))).toBeNull();
    expect(entero(new FormControl(2.5))).toEqual({ entero: true });
  });

  it('rangoFechas exige fin posterior al inicio', () => {
    const grupo = new FormGroup(
      { inicio: new FormControl('2027-03-01'), fin: new FormControl('2027-03-01') },
      { validators: rangoFechas('inicio', 'fin') },
    );
    expect(grupo.hasError('rangoFechas')).toBe(true);
    grupo.controls.fin.setValue('2027-07-15');
    expect(grupo.valid).toBe(true);
  });

  it('sinRepetidos detecta un curso repetido e ignora lineas sin elegir', () => {
    const linea = (cursoId: number) => new FormGroup({ cursoId: new FormControl(cursoId) });
    const lineas = new FormArray([linea(1), linea(0), linea(0)], [sinRepetidos('cursoId')]);
    expect(lineas.valid).toBe(true);
    lineas.push(linea(1));
    expect(lineas.hasError('repetido')).toBe(true);
  });

  it('alMenosUnaLinea', () => {
    const lineas = new FormArray<FormControl>([], [alMenosUnaLinea]);
    expect(lineas.hasError('sinLineas')).toBe(true);
    lineas.push(new FormControl(1));
    expect(lineas.valid).toBe(true);
  });
});
