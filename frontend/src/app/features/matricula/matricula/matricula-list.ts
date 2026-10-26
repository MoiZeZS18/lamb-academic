import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { LucideCalendarDays, LucideChevronDown, LucideChevronRight, LucideEye } from '@lucide/angular';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION } from '../../../core/auth/roles';
import { AvisoService } from '../../../core/services/aviso-service';
import { mensajeHttp } from '../../../core/services/error-http';
import { confirmar } from '../../../shared/componentes/confirmar-dialogo';
import { EstadoVacio } from '../../../shared/componentes/estado-vacio';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { resumenHorario } from '../../../shared/horario/horario-util';
import { Alumno, AlumnoService } from '../../base-universal/alumno/alumno-service';
import { Curso } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { Seccion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { motivosRevision } from './consistencia-matricula';
import { ESTADO_ANULADA, Matricula, Pagina } from './matricula.model';
import { MatriculaService } from './matricula-service';

/** Estados registrados en Oracle (el contrato los guarda como texto, PENDIENTES C6). */
const ESTADOS_MATRICULA = ['INICIADA', 'CONFIRMADA', 'ANULADA'];

/**
 * Consulta de matriculas (pantalla E): listado paginado en el servidor con filtros, detalle de
 * cursos desplegable, creditos y cargo. El ESTUDIANTE solo recibe las suyas (filtro del backend);
 * la coordinacion filtra por alumno y puede anular.
 */
@Component({
  selector: 'app-matricula-list',
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    CurrencyPipe,
    EtiquetaEstado,
    EstadoVacio,
    LucideCalendarDays,
    LucideChevronDown,
    LucideChevronRight,
    LucideEye,
  ],
  templateUrl: './matricula-list.html',
})
export class MatriculaList implements OnInit {
  private readonly matriculaService = inject(MatriculaService);
  private readonly periodoService = inject(PeriodoService);
  private readonly cursoService = inject(CursoService);
  private readonly seccionService = inject(SeccionService);
  private readonly alumnoService = inject(AlumnoService);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(AvisoService);
  protected readonly auth = inject(AuthService);

  protected readonly estados = ESTADOS_MATRICULA;
  protected readonly pagina = signal<Pagina<Matricula> | null>(null);
  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly alumnos = signal<Map<number, Alumno>>(new Map());
  protected readonly cursos = signal<Map<number, Curso>>(new Map());
  protected readonly secciones = signal<Map<number, Seccion>>(new Map());
  protected readonly expandida = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected periodoFiltro: number | null = null;
  protected estadoFiltro: string | null = null;
  protected alumnoFiltro: number | null = null;
  protected readonly numero = signal(0);
  protected tamano = 10;
  protected readonly tamanos = [10, 20, 50];
  protected readonly resumenHorario = resumenHorario;
  /** Coordinacion: texto para acotar la lista de alumnos del filtro. */
  protected readonly textoAlumno = signal('');
  protected readonly orden = signal<{ campo: string; direccion: 'asc' | 'desc' }>({ campo: 'fecha', direccion: 'desc' });

  protected readonly esCoordinacion = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));

  /** Motivos por los que una matricula activa requiere revision (registros previos a la integracion). */
  protected motivosRevision(m: Matricula): string[] {
    if (m.estado === ESTADO_ANULADA) return [];
    return motivosRevision(m, {
      alumnos: this.esCoordinacion() ? new Set(this.alumnos().keys()) : null,
      secciones: this.secciones(),
    });
  }
  protected readonly listaAlumnos = computed(() => {
    const texto = this.textoAlumno().trim().toLowerCase();
    const todos = [...this.alumnos().values()].sort((a, b) => a.persona.nombreCompleto.localeCompare(b.persona.nombreCompleto));
    if (!texto) return todos;
    return todos.filter(
      (a) =>
        a.id === this.alumnoFiltro ||
        `${a.codigoEstudiante} ${a.persona.nombreCompleto} ${a.persona.numeroDocumento}`.toLowerCase().includes(texto),
    );
  });

  protected hayFiltros(): boolean {
    return !!(this.periodoFiltro || this.estadoFiltro || this.alumnoFiltro);
  }

  ngOnInit(): void {
    forkJoin({
      periodos: this.periodoService.listar(),
      cursos: this.cursoService.listar(),
      secciones: this.seccionService.listar(),
      alumnos: this.esCoordinacion() ? this.alumnoService.listar() : of([] as Alumno[]),
    }).subscribe({
      next: ({ periodos, cursos, secciones, alumnos }) => {
        this.periodos.set([...periodos].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)));
        this.cursos.set(new Map(cursos.map((c) => [c.id, c])));
        this.secciones.set(new Map(secciones.map((s) => [s.id, s])));
        this.alumnos.set(new Map(alumnos.map((a) => [a.id, a])));
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.error.set(mensajeHttp(err, 'No se pudieron cargar los catálogos.')),
    });
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    const { campo, direccion } = this.orden();
    this.matriculaService
      .buscar(
        { periodoId: this.periodoFiltro, estado: this.estadoFiltro, estudianteId: this.alumnoFiltro },
        this.numero(),
        this.tamano,
        campo,
        direccion,
      )
      .subscribe({
        next: (pagina) => this.pagina.set(pagina),
        error: (err: HttpErrorResponse) => {
          this.error.set(mensajeHttp(err, 'No se pudo consultar las matrículas.'));
          this.loading.set(false);
        },
        complete: () => this.loading.set(false),
      });
  }

  filtrar(): void {
    this.numero.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.periodoFiltro = null;
    this.estadoFiltro = null;
    this.alumnoFiltro = null;
    this.textoAlumno.set('');
    this.filtrar();
  }

  ordenarPor(campo: string): void {
    const actual = this.orden();
    this.orden.set({ campo, direccion: actual.campo === campo && actual.direccion === 'desc' ? 'asc' : 'desc' });
    this.cargar();
  }

  irAPagina(numero: number): void {
    this.numero.set(numero);
    this.cargar();
  }

  alternar(id: number): void {
    this.expandida.set(this.expandida() === id ? null : id);
  }

  protected codigoPeriodo(id: number): string {
    return this.periodos().find((p) => p.id === id)?.codigo ?? `#${id}`;
  }

  protected alumno(id: number): string {
    const a = this.alumnos().get(id);
    return a ? `${a.codigoEstudiante} · ${a.persona.nombreCompleto}` : `Alumno ${id}`;
  }

  protected curso(id: number | null): string {
    if (id === null) return 'Sin identificar (registro previo)';
    const c = this.cursos().get(id);
    return c ? `${c.codigo} · ${c.nombre}` : `Curso ${id}`;
  }

  protected seccion(seccionId: number): Seccion | undefined {
    return this.secciones().get(seccionId);
  }

  protected indicadorOrden(campo: string): string {
    const o = this.orden();
    return o.campo === campo ? (o.direccion === 'desc' ? '↓' : '↑') : '';
  }

  anular(matricula: Matricula): void {
    confirmar(this.dialog, {
      titulo: `Anular matrícula ${matricula.id}`,
      mensaje:
        'Se liberarán los cupos de sus secciones y se anulará el cargo financiero (solo si no tiene pagos). ' +
        'El alumno podrá volver a matricularse en el periodo.',
      confirmar: 'Anular matrícula',
      peligro: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.matriculaService.anular(matricula.id).subscribe({
        next: () => {
          this.avisos.mostrar(`Matrícula ${matricula.id} anulada.`, 'ok');
          this.cargar();
        },
        error: (err: HttpErrorResponse) => this.error.set(mensajeHttp(err, 'No se pudo anular la matrícula.')),
      });
    });
  }

  protected readonly estadoAnulada = ESTADO_ANULADA;
}
