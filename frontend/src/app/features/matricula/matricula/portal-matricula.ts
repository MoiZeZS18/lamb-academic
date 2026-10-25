import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideCalendarDays, LucideCircleAlert, LucideClipboardPen, LucideEye } from '@lucide/angular';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION, Rol } from '../../../core/auth/roles';
import { mensajeHttp } from '../../../core/services/error-http';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { Curso } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { SeccionService } from '../seccion/seccion-service';
import { matriculaVigente, motivoBloqueo, periodoActivo } from './contexto-matricula';
import { Matricula, MatriculaReporte } from './matricula.model';
import { MatriculaService } from './matricula-service';

/**
 * Portal de matricula (pantalla A): periodo vigente, disponibilidad de inscripcion y, para el
 * estudiante, su matricula actual con creditos y cargo. Todo sale del backend: no hay fechas fijas.
 */
@Component({
  selector: 'app-portal-matricula',
  imports: [
    RouterLink,
    DatePipe,
    CurrencyPipe,
    EtiquetaEstado,
    LucideCalendarDays,
    LucideCircleAlert,
    LucideClipboardPen,
    LucideEye,
  ],
  templateUrl: './portal-matricula.html',
})
export class PortalMatricula implements OnInit {
  private readonly periodoService = inject(PeriodoService);
  private readonly matriculaService = inject(MatriculaService);
  private readonly seccionService = inject(SeccionService);
  private readonly cursoService = inject(CursoService);
  protected readonly auth = inject(AuthService);

  protected readonly periodo = signal<Periodo | null>(null);
  protected readonly matricula = signal<Matricula | null>(null);
  protected readonly cursos = signal<Map<number, Curso>>(new Map());
  protected readonly reporte = signal<MatriculaReporte | null>(null);
  protected readonly seccionesAbiertas = signal<number | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly esEstudiante = computed(() => this.auth.tieneRol(Rol.ESTUDIANTE) && this.auth.alumnoId() !== null);
  protected readonly esCoordinacion = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));
  protected readonly puedeInscribir = computed(() => this.esEstudiante() || this.esCoordinacion());
  protected readonly bloqueo = computed(() => motivoBloqueo(this.periodo()));

  ngOnInit(): void {
    if (this.auth.tieneRol(Rol.ESTUDIANTE) && this.auth.alumnoId() === null) {
      this.error.set(
        'Tu cuenta no está vinculada a ningún alumno. Solicita a la administración que registre tu código de alumno.',
      );
    }
    this.periodoService.listar().subscribe({
      next: (periodos) => {
        const activo = periodoActivo(periodos);
        this.periodo.set(activo);
        if (!activo) {
          this.loading.set(false);
          return;
        }
        this.cargarDelPeriodo(activo);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudo consultar el periodo académico.'));
        this.loading.set(false);
      },
    });
  }

  private cargarDelPeriodo(periodo: Periodo): void {
    forkJoin({
      // Para el estudiante el backend filtra por su alumno_id; aqui solo se acota el periodo.
      propias: this.esEstudiante() ? this.matriculaService.buscar({ periodoId: periodo.id }, 0, 20) : of(null),
      reporte: this.esCoordinacion() ? this.matriculaService.reporte({ periodoId: periodo.id }) : of(null),
      secciones: this.seccionService.listar({ periodoId: periodo.id, estado: 'ABIERTA' }),
      cursos: this.cursoService.listar(),
    }).subscribe({
      next: ({ propias, reporte, secciones, cursos }) => {
        this.matricula.set(propias ? matriculaVigente(propias.content) : null);
        this.reporte.set(reporte);
        this.seccionesAbiertas.set(secciones.length);
        this.cursos.set(new Map(cursos.map((c) => [c.id, c])));
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudo consultar la información de matrícula.'));
        this.loading.set(false);
      },
    });
  }

  protected nombreCurso(id: number | null): string {
    if (id === null) return 'Curso sin identificar (registro anterior a la integración)';
    const curso = this.cursos().get(id);
    return curso ? `${curso.codigo} · ${curso.nombre}` : `Curso ${id}`;
  }
}
