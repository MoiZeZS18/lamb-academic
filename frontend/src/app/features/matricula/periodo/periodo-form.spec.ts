import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { PeriodoForm } from './periodo-form';

const API = 'http://localhost:8080/api/v1/periodos';

describe('PeriodoForm (CRUD independiente, S7)', () => {
  let backend: HttpTestingController;
  let otros: object[];

  async function crear(id?: string) {
    await TestBed.configureTestingModule({
      imports: [PeriodoForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } } },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PeriodoForm);
    // Lista de periodos para avisar si ya hay otro ACTIVO.
    backend.expectOne((r) => r.method === 'GET' && r.url === API).flush(otros);
    await fixture.whenStable();
    return fixture;
  }

  function llenar(html: HTMLElement, valores: Record<string, string>) {
    for (const [id, valor] of Object.entries(valores)) {
      const campo = html.querySelector<HTMLInputElement | HTMLSelectElement>(`#${id}`)!;
      campo.value = valor;
      campo.dispatchEvent(new Event(campo.tagName === 'SELECT' ? 'change' : 'input'));
    }
  }

  beforeEach(() => {
    otros = [];
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('no envia un periodo invalido y explica cada campo', async () => {
    const fixture = await crear();
    const html = fixture.nativeElement as HTMLElement;
    llenar(html, { codigo: '2027-I', fechaInicio: '2027-07-15', fechaFin: '2027-03-01' });
    html.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
    expect(html.textContent).toContain('La fecha de fin debe ser posterior a la fecha de inicio.');
    expect(html.textContent).toContain('Este campo es obligatorio.');
    backend.expectNone((r) => r.method === 'POST');
  });

  it('crea un periodo valido y vuelve a la lista', async () => {
    const fixture = await crear();
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const html = fixture.nativeElement as HTMLElement;
    llenar(html, { codigo: ' 2027-I ', nombre: 'Periodo 2027-I', fechaInicio: '2027-03-01', fechaFin: '2027-07-15' });
    html.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    const req = backend.expectOne((r) => r.method === 'POST' && r.url === API);
    expect(req.request.body).toEqual({
      codigo: '2027-I',
      nombre: 'Periodo 2027-I',
      fechaInicio: '2027-03-01',
      fechaFin: '2027-07-15',
      estado: 'PLANIFICADO',
    });
    req.flush({ id: 30, ...req.request.body });
    expect(navegar).toHaveBeenCalledWith(['/matricula/periodos']);
  });

  it('al editar carga el periodo y muestra el 409 real del backend', async () => {
    const fixture = await crear('4');
    backend.expectOne(`${API}/4`).flush({
      id: 4,
      codigo: '2026-II',
      nombre: 'Periodo 2026-II',
      fechaInicio: '2026-08-01',
      fechaFin: '2026-12-20',
      estado: 'ACTIVO',
    });
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector<HTMLInputElement>('#codigo')!.value).toBe('2026-II');
    html.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    backend
      .expectOne(`${API}/4`)
      .flush({ status: 409, message: 'Ya existe otro periodo con codigo 2026-II' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    expect(html.querySelector('.mensaje-error')!.textContent).toContain('Ya existe otro periodo con codigo 2026-II');
  });

  it('avisa al editar un periodo CERRADO y al activar uno cuando ya hay otro activo', async () => {
    otros = [
      { id: 4, codigo: '2026-II', nombre: 'P', fechaInicio: '2026-08-01', fechaFin: '2026-12-20', estado: 'ACTIVO' },
      { id: 24, codigo: '2026-I', nombre: 'P', fechaInicio: '2026-03-01', fechaFin: '2026-07-15', estado: 'CERRADO' },
    ];
    const fixture = await crear('24');
    backend.expectOne(`${API}/24`).flush(otros[1]);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Este periodo está cerrado');
    expect(html.textContent).not.toContain('también está activo');
    const estado = html.querySelector<HTMLSelectElement>('#estado')!;
    estado.value = [...estado.options].find((o) => o.text.trim() === 'ACTIVO')!.value;
    estado.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    expect(html.textContent).toContain('El periodo 2026-II también está activo');
  });
});
