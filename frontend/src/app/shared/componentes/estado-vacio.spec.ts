import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EstadoVacio } from './estado-vacio';

@Component({
  imports: [EstadoVacio],
  template: `
    <app-estado-vacio titulo="Sin matrículas" descripcion="No hay matrículas con esos filtros.">
      <button type="button">Limpiar filtros</button>
    </app-estado-vacio>
    <app-estado-vacio titulo="Solo título" />
  `,
})
class Anfitrion {}

describe('EstadoVacio', () => {
  it('anuncia el estado, explica el motivo y proyecta la accion siguiente', async () => {
    await TestBed.configureTestingModule({ imports: [Anfitrion] }).compileComponents();
    const fixture = TestBed.createComponent(Anfitrion);
    await fixture.whenStable();
    const [conAccion, simple] = [...(fixture.nativeElement as HTMLElement).querySelectorAll('[role=status]')];
    expect(conAccion.querySelector('strong')!.textContent).toBe('Sin matrículas');
    expect(conAccion.querySelector('p')!.textContent).toContain('No hay matrículas con esos filtros.');
    expect(conAccion.querySelector('button')!.textContent).toBe('Limpiar filtros');
    expect(simple.querySelector('p')).toBeNull();
  });
});
