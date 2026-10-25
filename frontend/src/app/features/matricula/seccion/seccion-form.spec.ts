import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { SeccionForm } from './seccion-form';

const API = 'http://localhost:8080/api/v1';
const CURSOS = [
  { id: 6, codigo: 'IS202', nombre: 'Ingenieria de Software', creditos: 3, horasTeoricas: 2, horasPracticas: 2, activo: true },
];
const PERIODOS = [
  { id: 4, codigo: '2026-II', nombre: 'P', fechaInicio: '2026-08-01', fechaFin: '2026-12-20', estado: 'ACTIVO' },
  { id: 24, codigo: '2026-I', nombre: 'P', fechaInicio: '2026-03-01', fechaFin: '2026-07-15', estado: 'CERRADO' },
];

describe('SeccionForm (CRUD dependiente, S8)', () => {
  let backend: HttpTestingController;

  async function crear(cursos = CURSOS) {
    iniciarSesionDePrueba(['COORDINADOR']);
    await TestBed.configureTestingModule({
      imports: [SeccionForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SeccionForm);
    backend.expectOne(`${API}/cursos`).flush(cursos);
    backend.expectOne(`${API}/periodos`).flush(PERIODOS);
    backend.expectOne((r) => r.url === `${API}/docentes`).flush([{ id: 5, nombreCompleto: 'Vargas Choque Elena', email: null, activo: true }]);
    await fixture.whenStable();
    return fixture;
  }

  function componente(fixture: { componentInstance: SeccionForm }) {
    return fixture.componentInstance as unknown as {
      form: { controls: Record<string, { setValue(v: unknown): void; value: unknown }> };
      guardar(): void;
      onCursoSeleccionado(e: unknown): void;
      error(): string | null;
      cursoBusqueda(): string;
    };
  }

  beforeEach(() => {
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('lista desplegable de periodos: solo los no cerrados, con el activo preseleccionado', async () => {
    const fixture = await crear();
    const opciones = [...(fixture.nativeElement as HTMLElement).querySelectorAll('#periodo option')].map((o) => o.textContent!.trim());
    expect(opciones).toEqual(['Selecciona un periodo', '2026-II · ACTIVO']);
  });

  it('sin curso elegido no envia nada (validacion de la dependencia en el formulario)', async () => {
    const fixture = await crear();
    const c = componente(fixture);
    c.form.controls['grupo'].setValue('B');
    (fixture.componentInstance as unknown as { horariosForm: { at(i: number): { controls: { aula: { setValue(v: string): void } } } } })
      .horariosForm.at(0).controls.aula.setValue('A-301');
    c.guardar();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Selecciona un curso de la lista.');
    backend.expectNone(`${API}/secciones`);
  });

  it('escribir texto no basta: el curso solo se fija al elegir una opcion del autocomplete', async () => {
    const fixture = await crear();
    const c = componente(fixture);
    (fixture.componentInstance as unknown as { buscarCurso(t: string): void }).buscarCurso('Ingenieria');
    expect(c.form.controls['cursoId'].value).toBe(0);
    c.onCursoSeleccionado({ option: { value: 'IS202 · Ingenieria de Software' } });
    expect(c.form.controls['cursoId'].value).toBe(6);
  });

  it('curso eliminado mientras el formulario estaba abierto (404): mensaje especifico y recarga de opciones', async () => {
    const fixture = await crear();
    const c = componente(fixture);
    c.onCursoSeleccionado({ option: { value: 'IS202 · Ingenieria de Software' } });
    c.form.controls['grupo'].setValue('B');
    (fixture.componentInstance as unknown as { horariosForm: { at(i: number): { controls: { aula: { setValue(v: string): void } } } } })
      .horariosForm.at(0).controls.aula.setValue('A-301');
    c.guardar();
    const req = backend.expectOne(`${API}/secciones`);
    expect(req.request.body).toMatchObject({ cursoId: 6, periodoId: 4, grupo: 'B', docenteId: null, limiteVacantes: 30 });
    req.flush({ status: 404, message: 'Curso no encontrado: 6' }, { status: 404, statusText: 'Not Found' });
    backend.expectOne(`${API}/cursos`).flush([]);
    expect(c.error()).toBe('El curso seleccionado ya no existe. Elige otro de la lista.');
    expect(c.form.controls['cursoId'].value).toBe(0);
    expect(c.cursoBusqueda()).toBe('');
  });

  it('sin cursos activos avisa y deshabilita Guardar', async () => {
    const fixture = await crear([]);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('No hay cursos activos registrados');
    expect(html.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
  });
});
