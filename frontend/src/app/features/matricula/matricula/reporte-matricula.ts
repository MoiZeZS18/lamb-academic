import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucidePrinter } from '@lucide/angular';
import { forkJoin } from 'rxjs';
import { mensajeHttp } from '../../../core/services/error-http';
import { EstadoVacio } from '../../../shared/componentes/estado-vacio';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { CargoEstadoReporte, CargoService } from '../../finanzas/cargo/cargo-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { SeccionOcupacion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { periodoActivo } from './contexto-matricula';
import { ESTADO_ANULADA, MatriculaReporte } from './matricula.model';
import { MatriculaService } from './matricula-service';

/** Estados registrados en Oracle (PENDIENTES C6). INICIADA y CONFIRMADA son matriculas activas. */
const ESTADOS = ['INICIADA', 'CONFIRMADA', ESTADO_ANULADA];

/**
 * Indicadores financieros derivados del resumen por estado de Finanzas (GET /finanzas/cargos/resumen).
 * Reglas del backend que sostienen el calculo (CargoService): un cargo ANULADO queda con saldo 0 y
 * no puede tener pagos. Por eso lo pagado de un cargo es montoTotal - saldoPendiente solo en los
 * cargos no anulados, y lo anulado nunca se suma a la deuda.
 */
export function indicadoresCargos(cargos: CargoEstadoReporte[]) {
  const vigentes = cargos.filter((c) => c.estado !== 'ANULADO');
  const anulados = cargos.filter((c) => c.estado === 'ANULADO');
  const suma = (lista: CargoEstadoReporte[], f: (c: CargoEstadoReporte) => number) =>
    lista.reduce((t, c) => t + f(c), 0);
  return {
    cantidadHistorica: suma(cargos, (c) => c.cantidad),
    montoHistorico: suma(cargos, (c) => c.montoTotal),
    cantidadVigentes: suma(vigentes, (c) => c.cantidad),
    montoVigente: suma(vigentes, (c) => c.montoTotal),
    cantidadAnulados: suma(anulados, (c) => c.cantidad),
    montoAnulado: suma(anulados, (c) => c.montoTotal),
    pagado: suma(vigentes, (c) => c.montoTotal - c.saldoPendiente),
    saldoPendiente: suma(vigentes, (c) => c.saldoPendiente),
  };
}

/**
 * Consultas y reportes (S9 3.7 / S5): agregados reales del backend por periodo. Matricula
 * (/matriculas/reporte por estado), ocupacion de vacantes (/secciones/resumen) y cargos
 * (/finanzas/cargos/resumen). Nada se inventa: solo se suman o restan cifras del servidor.
 */
@Component({
  selector: 'app-reporte-matricula',
  imports: [FormsModule, CurrencyPipe, DecimalPipe, EtiquetaEstado, EstadoVacio, LucidePrinter],
  templateUrl: './reporte-matricula.html',
  styleUrl: './reporte-matricula.css',
})
export class ReporteMatricula implements OnInit {
  private readonly periodoService = inject(PeriodoService);
  private readonly matriculaService = inject(MatriculaService);
  private readonly seccionService = inject(SeccionService);
  private readonly cargoService = inject(CargoService);

  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly porEstado = signal<{ estado: string; reporte: MatriculaReporte }[]>([]);
  protected readonly ocupacion = signal<SeccionOcupacion[]>([]);
  protected readonly cargos = signal<CargoEstadoReporte[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected periodoFiltro: number | null = null;

  protected readonly matriculas = computed(() => {
    const filas = this.porEstado();
    const de = (activas: boolean) => filas.filter((f) => (f.estado === ESTADO_ANULADA) !== activas);
    const sumar = (lista: typeof filas, campo: keyof MatriculaReporte) =>
      lista.reduce((t, f) => t + f.reporte[campo], 0);
    const activas = de(true);
    const anuladas = de(false);
    const cantidadActivas = sumar(activas, 'totalMatriculas');
    const cursosActivos = sumar(activas, 'totalDetalles');
    return {
      activas: cantidadActivas,
      anuladas: sumar(anuladas, 'totalMatriculas'),
      cursosActivos,
      cursosPorMatricula: cantidadActivas ? cursosActivos / cantidadActivas : 0,
    };
  });

  protected readonly finanzas = computed(() => indicadoresCargos(this.cargos()));

  protected readonly totalesOcupacion = computed(() =>
    this.ocupacion().reduce(
      (t, o) => ({ capacidad: t.capacidad + o.vacantesTotales, ocupados: t.ocupados + o.cuposOcupados }),
      { capacidad: 0, ocupados: 0 },
    ),
  );

  ngOnInit(): void {
    this.periodoService.listar().subscribe({
      next: (periodos) => {
        this.periodos.set([...periodos].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)));
        this.periodoFiltro = periodoActivo(periodos)?.id ?? periodos[0]?.id ?? null;
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.error.set(mensajeHttp(err, 'No se pudieron cargar los periodos.')),
    });
  }

  cargar(): void {
    const periodoId = this.periodoFiltro;
    if (!periodoId) return;
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      estados: forkJoin(ESTADOS.map((estado) => this.matriculaService.reporte({ periodoId, estado }))),
      ocupacion: this.seccionService.ocupacion(periodoId),
      cargos: this.cargoService.resumen(periodoId),
    }).subscribe({
      next: ({ estados, ocupacion, cargos }) => {
        this.porEstado.set(ESTADOS.map((estado, i) => ({ estado, reporte: estados[i] })));
        this.ocupacion.set(ocupacion);
        this.cargos.set(cargos);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudieron generar los reportes.'));
        this.loading.set(false);
      },
    });
  }

  protected porcentaje(o: SeccionOcupacion): number {
    return o.vacantesTotales ? Math.round((o.cuposOcupados / o.vacantesTotales) * 100) : 0;
  }

  protected pagadoDe(c: CargoEstadoReporte): number | null {
    return c.estado === 'ANULADO' ? null : c.montoTotal - c.saldoPendiente;
  }

  protected codigoPeriodo(): string {
    return this.periodos().find((p) => p.id === this.periodoFiltro)?.codigo ?? '';
  }

  imprimir(): void {
    window.print();
  }
}
