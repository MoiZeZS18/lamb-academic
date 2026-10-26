import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  ALUMNOS_IDS,
  API,
  MATRICULAS_2026_II,
  PERIODO_ACTIVO,
  SECCIONES_2026_II,
  alumnosDePrueba,
  matriculaDePrueba,
  pagina,
  seccionDePrueba,
} from '../../../../testing/datos-matricula';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { PortalMatricula } from './portal-matricula';

const CARGO = {
  id: 9,
  numeroCargo: 'CRG-9',
  estudianteId: 1001,
  procesoOrigen: 'MATRICULA',
  referenciaId: 93,
  periodoId: 4,
  totalCreditos: 4,
  numeroCuotas: 2,
  montoTotal: 700,
  saldoPendiente: 500,
  estado: 'PAGADO_PARCIAL',
  observacion: null,
  fechaEmision: '2026-10-24T13:10:00',
  cuotas: [
    { id: 1, numeroCuota: 1, conceptoNombre: null, fechaVencimiento: '2026-11-10', montoCuota: 350, montoPagado: 200, estado: 'PAGADA_PARCIAL' },
    { id: 2, numeroCuota: 2, conceptoNombre: null, fechaVencimiento: '2026-12-10', montoCuota: 350, montoPagado: 0, estado: 'PENDIENTE' },
  ],
};

describe('PortalMatricula (pantalla A por rol)', () => {
  let backend: HttpTestingController;

  async function crear(roles: string[], alumnoId?: number) {
    iniciarSesionDePrueba(roles, alumnoId);
    await TestBed.configureTestingModule({
      imports: [PortalMatricula],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PortalMatricula);
    fixture.detectChanges();
    return fixture;
  }

  function texto(fixture: { nativeElement: unknown }): string {
    return ((fixture.nativeElement as HTMLElement).textContent ?? '').replace(/\s+/g, ' ');
  }

  function botonInscripcion(fixture: { nativeElement: unknown }) {
    return [...(fixture.nativeElement as HTMLElement).querySelectorAll('a')].find((a) =>
      a.textContent!.includes('Iniciar inscripción'),
    );
  }

  /** Contexto del estudiante (sin estudianteId: lo pone el backend) y plan vigente de su escuela. */
  function responderPlan() {
    const ctx = backend.expectOne(`${API}/matriculas/contexto`);
    expect(ctx.request.params.has('estudianteId')).toBe(false);
    ctx.flush({ estudianteId: 1001, existe: true, habilitado: true, escuelaId: 1 });
    backend.expectOne((r) => r.url === `${API}/planes-estudio` && r.params.get('idEscuela') === '1').flush([
      {
        id: 1,
        idEscuela: 1,
        escuelaNombre: 'Escuela Profesional de Ingenieria de Sistemas',
        codigo: 'PLAN-EPIS-2026',
        nombre: 'Plan EPIS 2026',
        anio: 2026,
        version: 1,
        estado: 'VIGENTE',
        totalCreditos: 4,
        totalCursos: 1,
        detalles: [
          { id: 1, idCurso: 1001, cursoCodigo: 'IS101', cursoNombre: 'Curso IS101', ciclo: 3, creditos: 4, tipo: 'OBLIGATORIO', idPrerrequisito: null, prerrequisitoCodigo: null },
        ],
      },
    ]);
  }

  beforeEach(() => {
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('el ESTUDIANTE ve su matricula, sus cursos y el saldo real de su cargo', async () => {
    const fixture = await crear(['ESTUDIANTE'], 1001);
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO]);
    const matricula = matriculaDePrueba(93, {
      cargo: { cargoId: 9, numeroCargo: 'CRG-9', montoTotal: 700, numeroCuotas: 2, estado: 'PAGADO_PARCIAL' },
    });
    backend.expectOne((r) => r.url === `${API}/matriculas`).flush(pagina([matricula]));
    backend.expectOne((r) => r.url === `${API}/secciones`).flush([seccionDePrueba(1001, 'IS101')]);
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([]);
    backend.expectOne(`${API}/finanzas/cargos/9`).flush(CARGO);
    responderPlan();
    await fixture.whenStable();

    const contenido = texto(fixture);
    expect(contenido).toContain('Mi matrícula 2026-II');
    expect(contenido).toContain('IS101');
    expect(contenido).toContain('Saldo pendiente');
    expect(contenido).toMatch(/500\.00/);
    // Proxima cuota: lo que falta de la primera cuota no pagada del todo (350 - 200).
    expect(contenido).toMatch(/10\/11\/2026 · S\/\s?150\.00/);
    expect(botonInscripcion(fixture)).toBeUndefined();
    // Ubicacion en su plan, resultado academico y lo pagado (suma de cuotas: 200 + 0).
    expect(contenido).toContain('Ciclo 3 de tu plan');
    expect(contenido).toContain('PLAN-EPIS-2026');
    expect(contenido).toContain('En curso');
    expect(contenido).toMatch(/Pagado\s?S\/\s?200\.00/);
    // El estudiante no consulta los reportes del periodo (ni siquiera los pide).
    backend.expectNone((r) => r.url.includes('/reporte'));
  });

  it('el ESTUDIANTE sin matricula puede iniciar la inscripcion si el periodo esta activo', async () => {
    const fixture = await crear(['ESTUDIANTE'], 1033);
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO]);
    backend.expectOne((r) => r.url === `${API}/matriculas`).flush(pagina([]));
    backend.expectOne((r) => r.url === `${API}/secciones`).flush([seccionDePrueba(1001, 'IS101')]);
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([]);
    responderPlan();
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Aún no tienes matrícula en 2026-II');
    expect(botonInscripcion(fixture)).toBeDefined();
  });

  it('sin periodo activo no ofrece iniciar la inscripcion', async () => {
    const fixture = await crear(['ESTUDIANTE'], 1033);
    backend.expectOne(`${API}/periodos`).flush([{ ...PERIODO_ACTIVO, estado: 'CERRADO' }]);
    backend.expectOne((r) => r.url === `${API}/matriculas`).flush(pagina([]));
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([]);
    responderPlan();
    await fixture.whenStable();

    expect(texto(fixture)).toContain('No hay un periodo activo');
    expect(botonInscripcion(fixture)).toBeUndefined();
  });

  it('la COORDINACION ve indicadores consistentes y la trazabilidad con lo registrado', async () => {
    const fixture = await crear(['COORDINADOR']);
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO]);
    backend
      .expectOne((r) => r.url === `${API}/secciones`)
      .flush([
        ...SECCIONES_2026_II,
        seccionDePrueba(1006, 'IS103', { limiteVacantes: 2, cuposOcupados: 2, vacantesDisponibles: 0 }),
      ]);
    backend
      .expectOne((r) => r.url === `${API}/matriculas` && r.params.get('periodoId') === '4')
      .flush(pagina(MATRICULAS_2026_II));
    backend.expectOne(`${API}/alumnos`).flush(alumnosDePrueba(ALUMNOS_IDS));
    await fixture.whenStable();

    const indicadores = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.indicador')].map((i) =>
      [...i.children].map((parte) => parte.textContent!.trim()).join(' '),
    );
    expect(indicadores).toContain('2 Matrículas activas consistentes');
    expect(indicadores).toContain('5 Inscripciones a cursos (consistentes)');
    expect(indicadores).toContain('1 Matrículas anuladas');
    expect(indicadores).toContain('116 Vacantes disponibles en secciones abiertas');
    expect(texto(fixture)).toContain(
      'El servidor registra 9 matrículas activas: 2 consistentes y 7 registros históricos que requieren revisión',
    );
    expect(texto(fixture)).toContain('Secciones abiertas sin vacantes');
    // La coordinacion ya no pide los agregados por estado: revisa cada registro.
    backend.expectNone((r) => r.url.includes('/matriculas/reporte'));
  });
});
