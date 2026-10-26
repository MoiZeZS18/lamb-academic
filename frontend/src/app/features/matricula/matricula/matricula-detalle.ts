import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { LucideCalendarDays, LucidePrinter, LucideReceipt } from '@lucide/angular';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION } from '../../../core/auth/roles';
import { AvisoService } from '../../../core/services/aviso-service';
import { mensajeHttp } from '../../../core/services/error-http';
import { confirmar } from '../../../shared/componentes/confirmar-dialogo';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { resumenHorario } from '../../../shared/horario/horario-util';
import { AlumnoService } from '../../base-universal/alumno/alumno-service';
import { Curso } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { Cargo, CargoService } from '../../finanzas/cargo/cargo-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { Seccion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { motivosRevision } from './consistencia-matricula';
import { ESTADO_ANULADA, Matricula, ResultadoCurso } from './matricula.model';
import { MatriculaService } from './matricula-service';

/**
 * Detalle de una matricula: cabecera, cursos con su seccion y horario, y el cargo financiero
 * relacionado con sus cuotas (Finanzas). Coordinacion: anular, generar el cargo que falte y
 * registrar resultados cuando el periodo esta CERRADO.
 */
@Component({
  selector: 'app-matricula-detalle',
  imports: [RouterLink, FormsModule, DatePipe, CurrencyPipe, EtiquetaEstado, LucideCalendarDays, LucidePrinter, LucideReceipt],
  templateUrl: './matricula-detalle.html',
})
export class MatriculaDetalle {
  /** :id de la ruta (withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly matriculaService = inject(MatriculaService);
  private readonly periodoService = inject(PeriodoService);
  private readonly seccionService = inject(SeccionService);
  private readonly cursoService = inject(CursoService);
  private readonly cargoService = inject(CargoService);
  private readonly alumnoService = inject(AlumnoService);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(AvisoService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly resumenHorario = resumenHorario;
  protected readonly resultados: ResultadoCurso[] = ['APROBADO', 'DESAPROBADO', 'RETIRADO'];

  protected readonly matricula = signal<Matricula | null>(null);
  protected readonly periodo = signal<Periodo | null>(null);
  protected readonly secciones = signal<Map<number, Seccion>>(new Map());
  protected readonly cursos = signal<Map<number, Curso>>(new Map());
  protected readonly cargo = signal<Cargo | null>(null);
  protected readonly nombreAlumno = signal<string>('');
  /** Motivos de revision (registro historico inconsistente); vacio si la matricula es consistente. */
  protected readonly motivosRevision = signal<string[]>([]);
  protected readonly loading = signal(true);
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errorCargo = signal<string | null>(null);

  protected readonly esCoordinacion = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));
  /** Lo realmente pagado (suma de cuotas): un cargo anulado tiene saldo 0 sin haberse pagado. */
  protected readonly totalPagado = computed(() =>
    (this.cargo()?.cuotas ?? []).reduce((total, cuota) => total + cuota.montoPagado, 0),
  );
  protected readonly anulada = computed(() => this.matricula()?.estado === ESTADO_ANULADA);
  protected readonly puedeRegistrarResultado = computed(
    () => this.esCoordinacion() && !this.anulada() && this.periodo()?.estado === 'CERRADO',
  );

  /** Numero de la ultima carga: las respuestas de una carga anterior se descartan. */
  private ultimaCarga = 0;

  constructor() {
    // El router reutiliza el componente al pasar de /consulta/93 a /consulta/1: se recarga con cada :id.
    effect(() => {
      const id = this.id();
      untracked(() => {
        this.matricula.set(null);
        this.periodo.set(null);
        this.cargo.set(null);
        this.nombreAlumno.set('');
        this.motivosRevision.set([]);
        this.cargar(id);
      });
    });
  }

  private cargar(id = this.id()): void {
    const carga = ++this.ultimaCarga;
    const vigente = () => carga === this.ultimaCarga;
    this.loading.set(true);
    this.error.set(null);
    this.errorCargo.set(null);
    this.matriculaService.obtener(Number(id)).subscribe({
      next: (matricula) => {
        if (!vigente()) return;
        this.matricula.set(matricula);
        forkJoin({
          periodo: this.periodoService.obtener(matricula.periodoId),
          secciones: this.seccionService.listar({ periodoId: matricula.periodoId }),
          cursos: this.cursoService.listar(),
          cargo: matricula.cargo ? this.cargoService.obtener(matricula.cargo.cargoId) : of(null),
          alumnos: this.esCoordinacion() ? this.alumnoService.listar() : of([]),
        }).subscribe({
          next: ({ periodo, secciones, cursos, cargo, alumnos }) => {
            if (!vigente()) return;
            const alumno = alumnos.find((a) => a.id === matricula.estudianteId);
            this.nombreAlumno.set(
              alumno
                ? `${alumno.codigoEstudiante} · ${alumno.persona.nombreCompleto}`
                : matricula.estudianteId === this.auth.alumnoId()
                  ? (this.auth.usuario()?.nombre ?? '')
                  : `Alumno ${matricula.estudianteId}`,
            );
            this.periodo.set(periodo);
            this.secciones.set(new Map(secciones.map((s) => [s.id, s])));
            this.cursos.set(new Map(cursos.map((c) => [c.id, c])));
            this.cargo.set(cargo);
            this.motivosRevision.set(
              matricula.estado === ESTADO_ANULADA
                ? []
                : motivosRevision(matricula, {
                    alumnos: this.esCoordinacion() ? new Set(alumnos.map((a) => a.id)) : null,
                    secciones: this.secciones(),
                  }),
            );
            this.loading.set(false);
          },
          error: (err: HttpErrorResponse) => {
            if (!vigente()) return;
            this.errorCargo.set(mensajeHttp(err, 'No se pudo consultar el cargo o la oferta del periodo.'));
            this.loading.set(false);
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        if (!vigente()) return;
        this.error.set(
          err.status === 404 ? `La matrícula ${id} no existe.` : mensajeHttp(err, 'No se pudo consultar la matrícula.'),
        );
        this.loading.set(false);
      },
    });
  }

  protected seccion(id: number): Seccion | undefined {
    return this.secciones().get(id);
  }

  protected curso(id: number | null): string {
    if (id === null) return 'Curso sin identificar (registro anterior a la integración)';
    const c = this.cursos().get(id);
    return c ? `${c.codigo} · ${c.nombre}` : `Curso ${id}`;
  }

  anular(): void {
    const m = this.matricula();
    if (!m) return;
    confirmar(this.dialog, {
      titulo: `Anular matrícula ${m.id}`,
      mensaje:
        'Se liberarán los cupos de sus secciones y se anulará el cargo (solo si no tiene pagos registrados).',
      confirmar: 'Anular matrícula',
      peligro: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.ejecutar(this.matriculaService.anular(m.id), `Matrícula ${m.id} anulada.`, 'No se pudo anular la matrícula.');
    });
  }

  generarCargo(): void {
    const m = this.matricula();
    if (!m) return;
    this.ejecutar(this.matriculaService.generarCargo(m.id), 'Cargo financiero generado.', 'No se pudo generar el cargo.');
  }

  registrarResultado(detalleId: number, resultado: ResultadoCurso): void {
    const m = this.matricula();
    if (!m) return;
    this.ejecutar(
      this.matriculaService.registrarResultado(m.id, detalleId, resultado),
      `Resultado ${resultado} registrado.`,
      'No se pudo registrar el resultado.',
    );
  }

  imprimir(): void {
    window.print();
  }

  volver(): void {
    this.router.navigate(['/matricula/consulta']);
  }

  private ejecutar(peticion: ReturnType<MatriculaService['anular']>, ok: string, generico: string): void {
    this.procesando.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: () => {
        this.avisos.mostrar(ok, 'ok');
        this.procesando.set(false);
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, generico));
        this.procesando.set(false);
      },
    });
  }
}
