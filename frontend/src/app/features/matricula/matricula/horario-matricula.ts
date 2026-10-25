import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucidePrinter } from '@lucide/angular';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION } from '../../../core/auth/roles';
import { mensajeHttp } from '../../../core/services/error-http';
import { BloqueSemanal, HorarioSemanal } from '../../../shared/horario/horario-semanal';
import { NOMBRE_DIA, aMinutos } from '../../../shared/horario/horario-util';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { Seccion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { ESTADO_ANULADA, Matricula } from './matricula.model';
import { MatriculaService } from './matricula-service';

interface ClaseDelHorario {
  seccion: Seccion;
  dia: string;
  orden: number;
  inicio: string;
  fin: string;
  aula: string;
}

/**
 * Horario academico (pantalla D): vista semanal de las secciones de una matricula real.
 * Sin matricula no se dibujan clases. Filtros: matricula (o periodo), curso y datos visibles.
 */
@Component({
  selector: 'app-horario-matricula',
  imports: [RouterLink, FormsModule, HorarioSemanal, LucidePrinter],
  templateUrl: './horario-matricula.html',
})
export class HorarioMatricula implements OnInit {
  /** ?matriculaId= (withComponentInputBinding). */
  readonly matriculaId = input<string | undefined>();

  private readonly matriculaService = inject(MatriculaService);
  private readonly seccionService = inject(SeccionService);
  private readonly periodoService = inject(PeriodoService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly matriculas = signal<Matricula[]>([]);
  protected readonly periodos = signal<Map<number, Periodo>>(new Map());
  protected readonly matricula = signal<Matricula | null>(null);
  protected readonly secciones = signal<Map<number, Seccion>>(new Map());
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly cursoFiltro = signal<number | null>(null);
  protected readonly mostrarAula = signal(true);
  protected readonly mostrarDocente = signal(true);
  protected numeroBuscado: number | null = null;

  protected readonly esCoordinacion = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));

  /** Secciones de la matricula seleccionada que existen en la oferta del periodo. */
  protected readonly seccionesMatricula = computed(() => {
    const m = this.matricula();
    if (!m) return [];
    return m.detalles.map((d) => this.secciones().get(d.seccionId)).filter((s): s is Seccion => !!s);
  });

  protected readonly sinHorario = computed(() => {
    const m = this.matricula();
    return m ? m.detalles.filter((d) => !this.secciones().has(d.seccionId)).length : 0;
  });

  protected readonly bloques = computed<BloqueSemanal[]>(() =>
    this.seccionesMatricula()
      .filter((s) => !this.cursoFiltro() || s.curso.id === this.cursoFiltro())
      .flatMap((s) =>
        s.horarios.map((h) => ({
          clave: String(s.id),
          dia: h.diaSemana,
          inicio: h.horaInicio,
          fin: h.horaFin,
          titulo: `${s.curso.codigo} · ${s.grupo}`,
          subtitulo: this.mostrarAula() ? `Aula ${h.aula}` : undefined,
          detalle: this.mostrarDocente() ? (s.docenteNombre ?? undefined) : undefined,
        })),
      ),
  );

  /** Misma informacion en formato de lista (accesible e imprimible). */
  protected readonly clases = computed<ClaseDelHorario[]>(() =>
    this.seccionesMatricula()
      .filter((s) => !this.cursoFiltro() || s.curso.id === this.cursoFiltro())
      .flatMap((s) =>
        s.horarios.map((h) => ({
          seccion: s,
          dia: NOMBRE_DIA[h.diaSemana],
          orden: ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'].indexOf(h.diaSemana) * 1440 + aMinutos(h.horaInicio),
          inicio: h.horaInicio,
          fin: h.horaFin,
          aula: h.aula,
        })),
      )
      .sort((a, b) => a.orden - b.orden),
  );

  ngOnInit(): void {
    this.periodoService.listar().subscribe({
      next: (periodos) => this.periodos.set(new Map(periodos.map((p) => [p.id, p]))),
    });
    const solicitado = Number(this.matriculaId()) || null;
    if (this.esCoordinacion()) {
      // La coordinacion consulta el horario de una matricula concreta (desde la consulta o por numero).
      if (solicitado) {
        this.numeroBuscado = solicitado;
        this.seleccionar(solicitado);
      } else {
        this.loading.set(false);
      }
      return;
    }
    this.matriculaService.buscar({}, 0, 50).subscribe({
      next: (pagina) => {
        const vigentes = pagina.content.filter((m) => m.estado !== ESTADO_ANULADA);
        this.matriculas.set(vigentes);
        const elegida = vigentes.find((m) => m.id === solicitado) ?? vigentes[0];
        if (elegida) {
          this.seleccionar(elegida.id);
        } else {
          this.loading.set(false);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudieron consultar tus matrículas.'));
        this.loading.set(false);
      },
    });
  }

  seleccionar(id: number | null): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.cursoFiltro.set(null);
    this.router.navigate([], { queryParams: { matriculaId: id }, replaceUrl: true });
    this.matriculaService.obtener(id).subscribe({
      next: (matricula) => {
        this.matricula.set(matricula);
        this.seccionService.listar({ periodoId: matricula.periodoId }).subscribe({
          next: (secciones) => {
            this.secciones.set(new Map(secciones.map((s) => [s.id, s])));
            this.loading.set(false);
          },
          error: (err: HttpErrorResponse) => {
            this.error.set(mensajeHttp(err, 'No se pudo consultar la oferta del periodo.'));
            this.loading.set(false);
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.matricula.set(null);
        this.error.set(
          err.status === 404 ? `La matrícula ${id} no existe.` : mensajeHttp(err, 'No se pudo consultar la matrícula.'),
        );
        this.loading.set(false);
      },
    });
  }

  protected etiquetaMatricula(m: Matricula): string {
    const periodo = this.periodos().get(m.periodoId)?.codigo ?? `periodo ${m.periodoId}`;
    return `${periodo} · matrícula ${m.id} (${m.estado})`;
  }

  imprimir(): void {
    window.print();
  }
}
