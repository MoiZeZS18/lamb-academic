import { AbstractControl, FormArray, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Numero entero (S8 3.7). */
export function entero(control: AbstractControl): ValidationErrors | null {
  return control.value === null || control.value === '' || Number.isInteger(Number(control.value))
    ? null
    : { entero: true };
}

/** En un grupo: la fecha `fin` debe ser posterior a `inicio` (fechas ISO, comparables como texto). */
export function rangoFechas(inicio: string, fin: string): ValidatorFn {
  return (grupo) => {
    const desde = grupo.get(inicio)?.value as string;
    const hasta = grupo.get(fin)?.value as string;
    return desde && hasta && hasta <= desde ? { rangoFechas: true } : null;
  };
}

/**
 * En un FormArray: ningun valor de `campo` se repite entre lineas (curso repetido en la matricula).
 * Patron tomado del formulario de malla de Cristian (coherenciaDetalles), adaptado a ValidatorFn.
 */
export function sinRepetidos(campo: string): ValidatorFn {
  return (control) => {
    const valores = (control as FormArray).controls
      .map((linea) => linea.get(campo)?.value)
      .filter((v) => v !== null && v !== undefined && v !== 0 && v !== '');
    return new Set(valores).size !== valores.length ? { repetido: true } : null;
  };
}

/** En un FormArray: al menos una linea (Validators.minLength no aplica a FormArray vacio en todas las versiones). */
export function alMenosUnaLinea(control: AbstractControl): ValidationErrors | null {
  return (control as FormArray).length > 0 ? null : { sinLineas: true };
}
