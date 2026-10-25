import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucidePrinter } from '@lucide/angular';
import { forkJoin } from 'rxjs';
import { mensajeHttp } from '../../../core/services/error-http';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { CargoEstadoReporte, CargoService } from '../../finanzas/cargo/cargo-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { SeccionOcupacion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { periodoActivo } from './contexto-matricula';
import { MatriculaReporte } from './matricula.model';
import { MatriculaService } from './matricula-service';

const ESTADOS = ['INICIADA', 'CONFIRMADA', 'ANULADA'];

/**
 * Consultas y reportes (S9 3.7 / S5): agregados reales del backend por periodo. Matricula
 * (/matriculas/reporte), ocupacion de vacantes (/secciones/resumen) y cargos (/finanzas/cargos/resumen).
 */
@Component({
  selector: 'app-reporte-matricula',
  imports: [FormsModule, CurrencyPipe, DecimalPipe, EtiquetaEstado, LucidePrinter],
  templateUrl: './reporte-matricula.html',
})
export class ReporteMatricula implements OnInit {
  private readonly periodoService = inject(PeriodoService);
  private readonly matriculaService = inject(MatriculaService);
  private readonly seccionService = inject(SeccionService);
  private readonly cargoService = inject(CargoService);

  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly total = signal<MatriculaReporte | null>(null);
  protected readonly porEstado = signal<{ estado: string; reporte: MatriculaReporte }[]>([]);
  protected readonly ocupacion = signal<SeccionOcupacion[]>([]);
  protected readonly cargos = signal<CargoEstadoReporte[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected periodoFiltro: number | null = null;

  protected readonly cursosPorMatricula = computed(() => {
    const t = this.total();
    return t && t.totalMatriculas ? t.totalDetalles / t.totalMatriculas : 0;
  });

  protected readonly totalesCargos = computed(() =>
    this.cargos().reduce(
      (acc, c) => ({ monto: acc.monto + c.montoTotal, saldo: acc.saldo + c.saldoPendiente, cantidad: acc.cantidad + c.cantidad }),
      { monto: 0, saldo: 0, cantidad: 0 },
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
      total: this.matriculaService.reporte({ periodoId }),
      estados: forkJoin(ESTADOS.map((estado) => this.matriculaService.reporte({ periodoId, estado }))),
      ocupacion: this.seccionService.ocupacion(periodoId),
      cargos: this.cargoService.resumen(periodoId),
    }).subscribe({
      next: ({ total, estados, ocupacion, cargos }) => {
        this.total.set(total);
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

  protected codigoPeriodo(): string {
    return this.periodos().find((p) => p.id === this.periodoFiltro)?.codigo ?? '';
  }

  imprimir(): void {
    window.print();
  }
}
