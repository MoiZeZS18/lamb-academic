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
  pagina,
} from '../../../../testing/datos-matricula';
import { CargoEstadoReporte } from '../../finanzas/cargo/cargo-service';
import { ReporteMatricula, indicadoresCargos } from './reporte-matricula';

const CARGOS: CargoEstadoReporte[] = [
  { estado: 'EMITIDO', cantidad: 1, montoTotal: 500, saldoPendiente: 500 },
  { estado: 'PAGADO_PARCIAL', cantidad: 1, montoTotal: 700, saldoPendiente: 500 },
  { estado: 'PAGADO_TOTAL', cantidad: 1, montoTotal: 300, saldoPendiente: 0 },
  { estado: 'ANULADO', cantidad: 2, montoTotal: 800, saldoPendiente: 0 },
];

describe('indicadoresCargos (reglas de Finanzas)', () => {
  it('separa lo historico, lo vigente y lo anulado', () => {
    const r = indicadoresCargos(CARGOS);
    expect(r.cantidadHistorica).toBe(5);
    expect(r.montoHistorico).toBe(2300);
    expect(r.cantidadVigentes).toBe(3);
    expect(r.montoVigente).toBe(1500);
    expect(r.cantidadAnulados).toBe(2);
    expect(r.montoAnulado).toBe(800);
  });

  it('lo anulado no es deuda ni pago: pagado = vigente - saldo', () => {
    const r = indicadoresCargos(CARGOS);
    expect(r.saldoPendiente).toBe(1000);
    expect(r.pagado).toBe(500);
    expect(r.pagado + r.saldoPendiente).toBe(r.montoVigente);
  });

  it('sin cargos todo es cero', () => {
    expect(indicadoresCargos([])).toEqual({
      cantidadHistorica: 0,
      montoHistorico: 0,
      cantidadVigentes: 0,
      montoVigente: 0,
      cantidadAnulados: 0,
      montoAnulado: 0,
      pagado: 0,
      saldoPendiente: 0,
    });
  });
});

describe('ReporteMatricula', () => {
  let backend: HttpTestingController;

  beforeEach(() => TestBed.resetTestingModule());
  afterEach(() => backend.verify());

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [ReporteMatricula],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ReporteMatricula);
    fixture.detectChanges();
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO]);
    // Agregados del servidor (incluyen los 7 registros historicos), igual que en Oracle.
    const reporte = (estado: string) =>
      backend.expectOne((r) => r.url === `${API}/matriculas/reporte` && r.params.get('estado') === estado);
    reporte('INICIADA').flush({ totalMatriculas: 9, totalDetalles: 14 });
    reporte('CONFIRMADA').flush({ totalMatriculas: 0, totalDetalles: 0 });
    reporte('ANULADA').flush({ totalMatriculas: 1, totalDetalles: 1 });
    backend
      .expectOne((r) => r.url === `${API}/secciones/resumen` && r.params.get('periodoId') === '4')
      .flush([{ cursoCodigo: 'IS103', cursoNombre: 'Analisis', secciones: 1, vacantesTotales: 6, cuposOcupados: 5 }]);
    backend
      .expectOne((r) => r.url === `${API}/finanzas/cargos/resumen` && r.params.get('periodoId') === '4')
      .flush(CARGOS);
    const registros = backend.expectOne(
      (r) => r.url === `${API}/matriculas` && r.params.get('periodoId') === '4' && r.params.get('size') === '100',
    );
    registros.flush(pagina(MATRICULAS_2026_II));
    backend.expectOne(`${API}/alumnos`).flush(alumnosDePrueba(ALUMNOS_IDS));
    backend.expectOne((r) => r.url === `${API}/secciones` && r.params.get('periodoId') === '4').flush(SECCIONES_2026_II);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function indicadores(html: HTMLElement) {
    return [...html.querySelectorAll('.indicador')].map((i) =>
      [...i.children].map((parte) => parte.textContent!.trim()).join(' '),
    );
  }

  it('cuenta solo las matriculas consistentes y conserva el total registrado', async () => {
    const html = await crear();
    const lista = indicadores(html);
    expect(lista).toContain('2 Matrículas activas consistentes');
    expect(lista).toContain('5 Inscripciones a cursos (consistentes)');
    expect(lista).toContain('7 Requieren revisión (registros históricos)');
    expect(lista).toContain('1 Matrículas anuladas');
    const texto = html.textContent!.replace(/\s+/g, ' ');
    expect(texto).toContain(
      'Total registrado en el servidor: 9 matrículas activas con 14 inscripciones = 2 consistentes (5 inscripciones) + 7 en revisión (9 inscripciones).',
    );
    expect(texto).not.toContain('no coincide con el total del servidor');
    // Conciliacion con la ocupacion de secciones.
    expect(texto).toContain('5 cupos ocupados frente a 5 inscripciones consistentes: coinciden.');
  });

  it('lista los registros que requieren revision con sus motivos y enlace al detalle', async () => {
    const html = await crear();
    const tabla = html.querySelector('section[aria-labelledby="t-revision"] table')!;
    const filas = [...tabla.querySelectorAll('tbody tr')];
    expect(filas.length).toBe(7);
    expect(filas[0].textContent).toContain('El alumno 1 no existe en Base Universal.');
    expect(filas[0].querySelector('a')!.getAttribute('href')).toBe('/matricula/consulta/1');
  });

  it('no calcula pagos sobre los cargos anulados', async () => {
    const html = await crear();
    expect(indicadores(html).some((i) => i.includes('Cargos anulados (2), no cobrables'))).toBe(true);
    const filaCargoAnulado = html.querySelector('section[aria-labelledby="t-cargos"] tr.anulada')!;
    expect(filaCargoAnulado.textContent).toContain('No aplica');
  });

  it('muestra capacidad, ocupados y disponibles = capacidad - ocupados', async () => {
    const html = await crear();
    const ocupacion = [...html.querySelectorAll('section[aria-labelledby="t-ocupacion"] tbody tr')].find((tr) =>
      tr.textContent!.includes('IS103'),
    )!;
    const celdas = [...ocupacion.querySelectorAll('td')].map((td) => td.textContent!.trim());
    expect(celdas.slice(1, 5)).toEqual(['1', '6', '5', '1']);
  });
});
