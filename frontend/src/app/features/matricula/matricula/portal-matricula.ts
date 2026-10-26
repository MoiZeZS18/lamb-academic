import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCalendarDays,
  LucideCalendarRange,
  LucideChartColumn,
  LucideCircleAlert,
  LucideClipboardPen,
  LucideEye,
  LucideLayers,
  LucideListChecks,
} from '@lucide/angular';
import { forkJoin, of, switchMap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION, Rol } from '../../../core/auth/roles';
import { mensajeHttp } from '../../../core/services/error-http';
import { EstadoVacio } from '../../../shared/componentes/estado-vacio';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { BloqueSemanal, HorarioSemanal } from '../../../shared/horario/horario-semanal';
import { resumenHorario } from '../../../shared/horario/horario-util';
import { Alumno, AlumnoService } from '../../base-universal/alumno/alumno-service';
import { Curso } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { DetallePlan, PlanEstudio } from '../../core-academico/plan/plan-estudio.model';
import { PlanEstudioService } from '../../core-academico/plan/plan-estudio-service';
import { Cargo, CargoService } from '../../finanzas/cargo/cargo-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { Seccion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { clasificarMatriculas } from './consistencia-matricula';
import { matriculaVigente, motivoBloqueo, periodoActivo } from './contexto-matricula';
import { Matricula } from './matricula.model';
import { MatriculaService } from './matricula-service';

/** Matriculas del periodo revisadas una por una (consistencia-matricula.ts). */
interface ResumenPeriodo {
  /** No anuladas, tal como las registra el servidor. */
  registradas: number;
  consistentes: number;
  revision: number;
  anuladas: number;
  cursosConsistentes: number;
}

/**
 * Portal de matricula (pantalla A). Todo sale del backend: periodo vigente y su disponibilidad,
 * y segun el rol, la matricula propia con sus cursos, cargo y horario (ESTUDIANTE) o los
 * indicadores del periodo con accesos a la gestion (COORDINADOR / ADMIN). Sin fechas fijas ni
 * estadisticas inventadas: si un dato no existe, se dice.
 */
@Component({
  selector: 'app-portal-matricula',
  imports: [
    RouterLink,
    DatePipe,
    CurrencyPipe,
    EtiquetaEstado,
    EstadoVacio,
    HorarioSemanal,
    LucideCalendarDays,
    LucideCalendarRange,
    LucideChartColumn,
    LucideCircleAlert,
    LucideClipboardPen,
    LucideEye,
    LucideLayers,
    LucideListChecks,
  ],
  templateUrl: './portal-matricula.html',
  styleUrl: './portal-matricula.css',
})
export class PortalMatricula implements OnInit {
  private readonly periodoService = inject(PeriodoService);
  private readonly matriculaService = inject(MatriculaService);
  private readonly seccionService = inject(SeccionService);
  private readonly cursoService = inject(CursoService);
  private readonly cargoService = inject(CargoService);
  private readonly alumnoService = inject(AlumnoService);
  private readonly planService = inject(PlanEstudioService);
  protected readonly auth = inject(AuthService);

  protected readonly resumenHorario = resumenHorario;
  protected readonly rolAdmin = Rol.ADMIN;

  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly periodo = signal<Periodo | null>(null);
  protected readonly misMatriculas = signal<Matricula[]>([]);
  protected readonly cargo = signal<Cargo | null>(null);
  protected readonly secciones = signal<Seccion[]>([]);
  protected readonly cursos = signal<Map<number, Curso>>(new Map());
  protected readonly resumen = signal<ResumenPeriodo | null>(null);
  protected readonly plan = signal<PlanEstudio | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly esEstudiante = computed(
    () => this.auth.tieneRol(Rol.ESTUDIANTE) && !this.auth.tieneRol(...ROLES_COORDINACION),
  );
  protected readonly esCoordinacion = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));
  protected readonly bloqueo = computed(() => motivoBloqueo(this.periodo()));

  /** Matricula vigente (no anulada) del estudiante en el periodo activo. */
  protected readonly matricula = computed(() => {
    const periodo = this.periodo();
    return periodo ? matriculaVigente(this.misMatriculas().filter((m) => m.periodoId === periodo.id)) : null;
  });

  /** Otras matriculas del estudiante (periodos anteriores o anuladas), para su historial. */
  protected readonly historial = computed(() => {
    const actual = this.matricula();
    return this.misMatriculas().filter((m) => m.id !== actual?.id);
  });

  private readonly seccionPorId = computed(() => new Map(this.secciones().map((s) => [s.id, s])));

  /** Cursos de la matricula vigente con su seccion (si existe en la oferta del periodo). */
  protected readonly cursosMatriculados = computed(() =>
    (this.matricula()?.detalles ?? []).map((d) => ({ detalle: d, seccion: this.seccionPorId().get(d.seccionId) })),
  );

  protected readonly bloquesHorario = computed<BloqueSemanal[]>(() =>
    this.cursosMatriculados().flatMap(({ seccion }) =>
      (seccion?.horarios ?? []).map((h) => ({
        clave: String(seccion!.id),
        dia: h.diaSemana,
        inicio: h.horaInicio,
        fin: h.horaFin,
        titulo: `${seccion!.curso.codigo} · ${seccion!.grupo}`,
        subtitulo: `Aula ${h.aula}`,
      })),
    ),
  );

  protected readonly planPorCurso = computed(
    () => new Map((this.plan()?.detalles ?? []).map((d): [number, DetallePlan] => [d.idCurso, d])),
  );

  /** Lo realmente pagado: suma de lo pagado en cada cuota (Finanzas). */
  protected readonly pagado = computed(() =>
    (this.cargo()?.cuotas ?? []).reduce((total, cuota) => total + cuota.montoPagado, 0),
  );

  protected readonly oferta = computed(() => {
    const vigentes = this.secciones().filter((s) => s.estado !== 'CANCELADA');
    return {
      abiertas: vigentes.filter((s) => s.estado === 'ABIERTA').length,
      total: vigentes.length,
      vacantesDisponibles: vigentes
        .filter((s) => s.estado === 'ABIERTA')
        .reduce((t, s) => t + s.vacantesDisponibles, 0),
      sinVacantes: vigentes
        .filter((s) => s.estado === 'ABIERTA' && s.vacantesDisponibles <= 0)
        .sort((a, b) => a.curso.codigo.localeCompare(b.curso.codigo)),
    };
  });

  ngOnInit(): void {
    if (this.auth.tieneRol(Rol.ESTUDIANTE) && !this.esCoordinacion() && this.auth.alumnoId() === null) {
      this.error.set(
        'Tu cuenta no está vinculada a ningún alumno. Solicita a la administración que registre tu código de alumno.',
      );
    }
    this.periodoService.listar().subscribe({
      next: (periodos) => {
        this.periodos.set(periodos);
        const activo = periodoActivo(periodos);
        this.periodo.set(activo);
        this.cargarDatos(activo);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudo consultar el periodo académico.'));
        this.loading.set(false);
      },
    });
  }

  private cargarDatos(periodo: Periodo | null): void {
    const estudiante = this.esEstudiante() && this.auth.alumnoId() !== null;
    const coordinacion = this.esCoordinacion() && periodo !== null;
    forkJoin({
      // Para el estudiante el backend devuelve solo sus matriculas (claim alumno_id).
      propias: estudiante ? this.matriculaService.buscar({}, 0, 50) : of(null),
      secciones: periodo ? this.seccionService.listar({ periodoId: periodo.id }) : of([] as Seccion[]),
      cursos: estudiante ? this.cursoService.listar() : of([] as Curso[]),
      delPeriodo: coordinacion ? this.matriculaService.buscarTodas({ periodoId: periodo!.id }) : of(null),
      alumnos: coordinacion ? this.alumnoService.listar() : of([] as Alumno[]),
    }).subscribe({
      next: (datos) => {
        this.misMatriculas.set(datos.propias?.content ?? []);
        this.secciones.set(datos.secciones);
        this.cursos.set(new Map(datos.cursos.map((c) => [c.id, c])));
        if (datos.delPeriodo) {
          const c = clasificarMatriculas(datos.delPeriodo, {
            alumnos: new Set(datos.alumnos.map((a) => a.id)),
            secciones: new Map(datos.secciones.map((s): [number, Seccion] => [s.id, s])),
          });
          this.resumen.set({
            registradas: c.registradas.length,
            consistentes: c.consistentes.length,
            revision: c.revision.length,
            anuladas: c.anuladas.length,
            cursosConsistentes: c.inscripcionesConsistentes,
          });
        }
        this.cargarCargo();
        if (estudiante) this.cargarPlan();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudo consultar la información de matrícula.'));
        this.loading.set(false);
      },
    });
  }

  private cargarPlan(): void {
    this.matriculaService
      .contexto()
      .pipe(
        switchMap((contexto) =>
          contexto.escuelaId ? this.planService.buscar(contexto.escuelaId, 'VIGENTE') : of([] as PlanEstudio[]),
        ),
      )
      .subscribe({
        next: (planes) => this.plan.set(planes[0] ?? null),
        error: () => this.plan.set(null),
      });
  }

  /** El cargo con su saldo real lo consulta Finanzas (el estudiante solo puede ver el suyo). */
  private cargarCargo(): void {
    const cargo = this.matricula()?.cargo;
    if (!cargo) {
      this.loading.set(false);
      return;
    }
    this.cargoService.obtener(cargo.cargoId).subscribe({
      next: (detalle) => {
        this.cargo.set(detalle);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected codigoPeriodo(id: number): string {
    return this.periodos().find((p) => p.id === id)?.codigo ?? `Periodo ${id}`;
  }

  protected nombreCurso(id: number | null): string {
    if (id === null) return 'Curso sin identificar (registro anterior a la integración)';
    const curso = this.cursos().get(id);
    return curso ? `${curso.codigo} · ${curso.nombre}` : `Curso ${id}`;
  }

  protected proximaCuota() {
    return this.cargo()?.cuotas.find((c) => c.estado === 'PENDIENTE' || c.estado === 'PAGADA_PARCIAL') ?? null;
  }
}
