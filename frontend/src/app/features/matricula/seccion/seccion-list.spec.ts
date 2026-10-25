import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { API, PERIODO_ACTIVO, PERIODO_CERRADO, seccionDePrueba } from '../../../../testing/datos-matricula';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { SeccionList } from './seccion-list';

describe('SeccionList (ocupacion y eliminacion)', () => {
  let backend: HttpTestingController;
  let dialogos: number;

  async function crear() {
    iniciarSesionDePrueba(['COORDINADOR']);
    await TestBed.configureTestingModule({
      imports: [SeccionList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: MatDialog,
          useValue: {
            open: () => {
              dialogos++;
              return { afterClosed: () => of(false) };
            },
          },
        },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SeccionList);
    fixture.detectChanges();
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([]);
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO, PERIODO_CERRADO]);
    backend
      .expectOne((r) => r.url === `${API}/secciones`)
      .flush([
        seccionDePrueba(1003, 'IS103', { limiteVacantes: 2, cuposOcupados: 2, vacantesDisponibles: 0 }),
        seccionDePrueba(1004, 'IS104', { limiteVacantes: 25, cuposOcupados: 0, vacantesDisponibles: 25 }),
        seccionDePrueba(1007, 'IS101', { periodoId: 24, cuposOcupados: 0, vacantesDisponibles: 30, estado: 'CERRADA' }),
      ]);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function fila(html: HTMLElement, codigo: string) {
    return [...html.querySelectorAll('tbody tr')].find((tr) => tr.textContent!.includes(codigo))!;
  }

  function botonEliminar(html: HTMLElement, codigo: string) {
    return [...fila(html, codigo).querySelectorAll('button')].find((b) => b.textContent!.includes('Eliminar'))!;
  }

  beforeEach(() => {
    dialogos = 0;
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('distingue ocupados, capacidad y disponibles con los valores del backend', async () => {
    const html = await crear();
    const cabeceras = [...html.querySelectorAll('thead th')].map((th) => th.textContent!.trim());
    expect(cabeceras).toContain('Ocupados / capacidad');
    expect(cabeceras).toContain('Disponibles');
    expect(fila(html, 'IS103').textContent).toContain('2 / 2');
    expect(fila(html, 'IS104').textContent).toContain('0 / 25');
  });

  it('no ofrece eliminar una seccion con matriculados ni una de periodo cerrado', async () => {
    const html = await crear();
    const ocupada = botonEliminar(html, 'IS103');
    expect(ocupada.disabled).toBe(true);
    expect(ocupada.title).toContain('Tiene matriculados');
    const cerrada = botonEliminar(html, 'IS101');
    expect(cerrada.disabled).toBe(true);
    expect(cerrada.title).toContain('periodo cerrado');
  });

  it('pide confirmacion antes de eliminar y, si se cancela, no envia el DELETE', async () => {
    const html = await crear();
    const libre = botonEliminar(html, 'IS104');
    expect(libre.disabled).toBe(false);
    libre.click();
    expect(dialogos).toBe(1);
    backend.expectNone((r) => r.method === 'DELETE');
  });
});
