import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { MatriculaList } from './matricula-list';

const API = 'http://localhost:8080/api/v1';
const PAGINA = {
  content: [
    {
      id: 93,
      estudianteId: 1001,
      periodoId: 4,
      fecha: '2026-10-24',
      estado: 'INICIADA',
      totalCreditos: 11,
      detalles: [{ id: 1, seccionId: 1001, cursoId: 1, creditos: 4, resultado: null }],
      cargo: { cargoId: 7, numeroCargo: 'CRG-7', montoTotal: 700, numeroCuotas: 5, estado: 'PAGADO_PARCIAL' },
    },
  ],
  page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
};

describe('MatriculaList (consulta paginada)', () => {
  let backend: HttpTestingController;

  async function crear(roles: string[], alumnoId?: number) {
    iniciarSesionDePrueba(roles, alumnoId);
    await TestBed.configureTestingModule({
      imports: [MatriculaList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MatriculaList);
    fixture.detectChanges();
    backend.expectOne(`${API}/periodos`).flush([{ id: 4, codigo: '2026-II', nombre: 'P', fechaInicio: '2026-08-01', fechaFin: '2026-12-20', estado: 'ACTIVO' }]);
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([{ id: 1, codigo: 'IS101', nombre: 'LP II', creditos: 4, activo: true }]);
    backend.expectOne((r) => r.url === `${API}/secciones`).flush([]);
    return fixture;
  }

  beforeEach(() => {
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('ESTUDIANTE: no consulta alumnos ni envia estudianteId (el backend filtra por su token)', async () => {
    const fixture = await crear(['ESTUDIANTE'], 1001);
    backend.expectNone(`${API}/alumnos`);
    const req = backend.expectOne((r) => r.url === `${API}/matriculas`);
    expect(req.request.params.has('estudianteId')).toBe(false);
    req.flush(PAGINA);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Mis matrículas');
    expect(html.textContent).toContain('S/700.00');
    expect(html.textContent).not.toContain('Anular');
  });

  it('COORDINADOR: filtra por alumno en el servidor, ve el cargo y la accion Anular', async () => {
    const fixture = await crear(['COORDINADOR']);
    backend.expectOne((r) => r.url === `${API}/alumnos`).flush([
      { id: 1001, codigoEstudiante: '2026100001', estado: 'ACTIVO', fechaIngreso: '2026-03-01', habilitado: true,
        persona: { id: 1, numeroDocumento: '1', nombreCompleto: 'Quispe Mamani Ana Lucia', email: null, idEscuela: 1, escuelaNombre: 'EPIS' } },
    ]);
    backend.expectOne((r) => r.url === `${API}/matriculas`).flush(PAGINA);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Quispe Mamani Ana Lucia');
    expect(html.textContent).toContain('Anular');

    const componente = fixture.componentInstance as unknown as { alumnoFiltro: number | null; filtrar(): void };
    componente.alumnoFiltro = 1001;
    componente.filtrar();
    const req = backend.expectOne((r) => r.url === `${API}/matriculas`);
    expect(req.request.params.get('estudianteId')).toBe('1001');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(PAGINA);
  });
});
