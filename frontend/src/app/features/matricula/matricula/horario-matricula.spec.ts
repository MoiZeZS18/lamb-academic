import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import {
  API,
  PERIODO_ACTIVO,
  matriculaDePrueba,
  pagina,
  seccionDePrueba,
} from '../../../../testing/datos-matricula';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { HorarioMatricula } from './horario-matricula';

type Interno = {
  alumnoBuscado: { set(v: number): void };
  buscar(): void;
  sinResultado(): string | null;
  bloques(): unknown[];
};

const SECCIONES = [
  seccionDePrueba(1001, 'IS101'),
  seccionDePrueba(1002, 'IS102', {
    horarios: [{ id: 1, diaSemana: 'MARTES', horaInicio: '08:00', horaFin: '10:00', aula: 'LAB-201' }],
  }),
];

describe('HorarioMatricula (horario semanal por rol)', () => {
  let backend: HttpTestingController;

  async function crear(roles: string[], alumnoId?: number) {
    iniciarSesionDePrueba(roles, alumnoId);
    await TestBed.configureTestingModule({
      imports: [HorarioMatricula],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(HorarioMatricula);
    fixture.detectChanges();
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO]);
    return { fixture, c: fixture.componentInstance as unknown as Interno };
  }

  beforeEach(() => {
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('el ESTUDIANTE ve su horario sin buscar y nunca el de una matricula anulada', async () => {
    const { fixture, c } = await crear(['ESTUDIANTE'], 1001);
    backend
      .expectOne((r) => r.url === `${API}/matriculas`)
      .flush(pagina([matriculaDePrueba(5, { estado: 'ANULADA' }), matriculaDePrueba(93)]));
    backend.expectOne(`${API}/matriculas/93`).flush(
      matriculaDePrueba(93, {
        detalles: [
          { id: 1, seccionId: 1001, cursoId: 1001, creditos: 4, resultado: null },
          { id: 2, seccionId: 1002, cursoId: 1002, creditos: 4, resultado: null },
        ],
      }),
    );
    backend.expectOne((r) => r.url === `${API}/secciones`).flush(SECCIONES);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('#alumno')).toBeNull();
    expect(c.bloques().length).toBe(2);
    expect(html.querySelectorAll('.bloque').length).toBe(2);
  });

  it('la COORDINACION busca por periodo y alumno: sin matricula lo explica', async () => {
    const { c } = await crear(['COORDINADOR']);
    backend.expectOne(`${API}/alumnos`).flush([]);
    c.alumnoBuscado.set(1033);
    c.buscar();
    const req = backend.expectOne((r) => r.url === `${API}/matriculas`);
    expect(req.request.params.get('periodoId')).toBe('4');
    expect(req.request.params.get('estudianteId')).toBe('1033');
    req.flush(pagina([]));
    expect(c.sinResultado()).toBe('El alumno no tiene matrícula en ese periodo.');
    expect(c.bloques().length).toBe(0);
  });

  it('la COORDINACION no muestra horario si el alumno solo tiene matriculas anuladas', async () => {
    const { c } = await crear(['COORDINADOR']);
    backend.expectOne(`${API}/alumnos`).flush([]);
    c.alumnoBuscado.set(1033);
    c.buscar();
    backend.expectOne((r) => r.url === `${API}/matriculas`).flush(pagina([matriculaDePrueba(7, { estado: 'ANULADA' })]));
    expect(c.sinResultado()).toContain('solo tiene matrículas anuladas');
    backend.expectNone(`${API}/matriculas/7`);
  });

  it('la COORDINACION abre la matricula vigente encontrada', async () => {
    const { fixture, c } = await crear(['COORDINADOR']);
    backend.expectOne(`${API}/alumnos`).flush([]);
    c.alumnoBuscado.set(1001);
    c.buscar();
    backend
      .expectOne((r) => r.url === `${API}/matriculas`)
      .flush(pagina([matriculaDePrueba(5, { estado: 'ANULADA' }), matriculaDePrueba(93)]));
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93));
    backend.expectOne((r) => r.url === `${API}/secciones`).flush(SECCIONES);
    await fixture.whenStable();
    expect(c.sinResultado()).toBeNull();
    // Solo la seccion 1001 esta en la matricula: no aparece la clase de IS102.
    expect(c.bloques().length).toBe(1);
  });
});
