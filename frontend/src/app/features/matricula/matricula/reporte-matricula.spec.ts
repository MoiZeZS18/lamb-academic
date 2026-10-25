import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API, PERIODO_ACTIVO } from '../../../../testing/datos-matricula';
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

  it('muestra activas y anuladas por separado y no calcula pagos sobre lo anulado', async () => {
    await TestBed.configureTestingModule({
      imports: [ReporteMatricula],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ReporteMatricula);
    fixture.detectChanges();
    backend.expectOne(`${API}/periodos`).flush([PERIODO_ACTIVO]);
    const reporte = (estado: string) =>
      backend.expectOne((r) => r.url === `${API}/matriculas/reporte` && r.params.get('estado') === estado);
    reporte('INICIADA').flush({ totalMatriculas: 9, totalDetalles: 14 });
    reporte('CONFIRMADA').flush({ totalMatriculas: 1, totalDetalles: 1 });
    reporte('ANULADA').flush({ totalMatriculas: 2, totalDetalles: 3 });
    backend
      .expectOne((r) => r.url === `${API}/secciones/resumen` && r.params.get('periodoId') === '4')
      .flush([{ cursoCodigo: 'IS103', cursoNombre: 'Analisis', secciones: 1, vacantesTotales: 2, cuposOcupados: 2 }]);
    backend
      .expectOne((r) => r.url === `${API}/finanzas/cargos/resumen` && r.params.get('periodoId') === '4')
      .flush(CARGOS);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    const indicadores = [...html.querySelectorAll('.indicador')].map((i) => [...i.children].map((parte) => parte.textContent!.trim()).join(' '));
    expect(indicadores).toContain('10 Matrículas activas (iniciadas y confirmadas)');
    expect(indicadores).toContain('2 Matrículas anuladas');
    expect(indicadores).toContain('15 Inscripciones a cursos en matrículas activas');
    expect(indicadores.some((i) => i.includes('Cargos anulados (2), no cobrables'))).toBe(true);

    const filaAnulada = html.querySelector('tr.anulada')!;
    expect(filaAnulada.textContent).toContain('No aplica');

    // Ocupacion: capacidad total, ocupados y disponibles = capacidad - ocupados.
    const ocupacion = [...html.querySelectorAll('tbody tr')].find((tr) => tr.textContent!.includes('IS103'))!;
    const celdas = [...ocupacion.querySelectorAll('td')].map((td) => td.textContent!.trim());
    expect(celdas.slice(1, 5)).toEqual(['1', '2', '2', '0']);
  });
});
