import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideEye, LucidePrinter } from '@lucide/angular';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION } from '../../../core/auth/roles';
import { mensajeHttp } from '../../../core/services/error-http';
import { EstadoVacio } from '../../../shared/componentes/estado-vacio';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { BloqueSemanal, HorarioSemanal } from '../../../shared/horario/horario-semanal';
import { NOMBRE_DIA, aMinutos } from '../../../shared/horario/horario-util';
import { Alumno, AlumnoService } from '../../base-universal/alumno/alumno-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { DIAS_SEMANA, Seccion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { matriculaVigente, periodoActivo } from './contexto-matricula';
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
 * - ESTUDIANTE: se arma solo con sus matriculas (el backend filtra por el alumno_id del token);
 *   si tiene varias, elige el periodo. Nunca escribe identificadores.
 * - COORDINACION: busca por periodo y alumno; tambien acepta ?matriculaId desde la consulta.
 * Sin matricula no se dibujan clases. Los permisos los vuelve a validar el backend.
 */
@Component({
  selector: 'app-horario-matricula',
  imports: [RouterLink, FormsModule, HorarioSemanal, EstadoVacio, EtiquetaEstado, LucideEye, LucidePrinter],
  templateUrl: './horario-matricula.html',
  styleUrl: './horario-matricula.css',
})
export class HorarioMatricula implements OnInit {
  /** ?matriculaId= (withComponentInputBinding). */
  readonly matriculaId = input<string | undefined>();

  private readonly matriculaService = inject(MatriculaService);
  private readonly seccionService = inject(SeccionService);
  private readonly periodoService = inject(PeriodoService);
  private readonly alumnoService = inject(AlumnoService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly alumnos = signal<Alumno[]>([]);
  /** ESTUDIANTE: sus matriculas vigentes (una por periodo). */
  protected readonly misMatriculas = signal<Matricula[]>([]);
  protected readonly matricula = signal<Matricula | null>(null);
  protected readonly secciones = signal<Map<number, Seccion>>(new Map());
  protected readonly loading = signal(true);
  protected readonly buscando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sinResultado = signal<string | null>(null);

  protected readonly cursoFiltro = signal<number | null>(null);
  protected readonly mostrarAula = signal(true);
  protected readonly mostrarDocente = signal(true);

  /** COORDINACION: criterios de busqueda. */
  protected readonly periodoBuscado = signal<number | null>(null);
  protected readonly alumnoBuscado = signal<number | null>(null);
  protected readonly textoAlumno = signal('');

  protected readonly esCoordinacion = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));

  protected readonly alumnosFiltrados = computed(() => {
    const texto = this.textoAlumno().trim().toLowerCase();
    const elegido = this.alumnoBuscado();
    if (!texto) return this.alumnos();
    return this.alumnos().filter(
      (a) =>
        a.id === elegido ||
        `${a.codigoEstudiante} ${a.persona.nombreCompleto} ${a.persona.numeroDocumento}`.toLowerCase().includes(texto),
    );
  });

  protected readonly periodoDeMatricula = computed(() => {
    const m = this.matricula();
    return m ? (this.periodos().find((p) => p.id === m.periodoId) ?? null) : null;
  });

  protected readonly alumnoDeMatricula = computed(() => {
    const m = this.matricula();
    if (!m) return '';
    if (m.estudianteId === this.auth.alumnoId()) return this.auth.usuario()?.nombre ?? '';
    const a = this.alumnos().find((x) => x.id === m.estudianteId);
    return a ? `${a.codigoEstudiante} · ${a.persona.nombreCompleto}` : `Alumno ${m.estudianteId}`;
  });

  /** Secciones de la matricula que existen en la oferta del periodo. */
  protected readonly seccionesMatricula = computed(() => {
    const m = this.matricula();
    if (!m) return [];
    return m.detalles.map((d) => this.secciones().get(d.seccionId)).filter((s): s is Seccion => !!s);
  });

  protected readonly sinHorario = computed(() => {
    const m = this.matricula();
    return m ? m.detalles.filter((d) => !this.secciones().has(d.seccionId)).length : 0;
  });

  private readonly seccionesVisibles = computed(() =>
    this.seccionesMatricula().filter((s) => !this.cursoFiltro() || s.curso.id === this.cursoFiltro()),
  );

  protected readonly bloques = computed<BloqueSemanal[]>(() =>
    this.seccionesVisibles().flatMap((s) =>
      s.horarios.map((h) => ({
        clave: String(s.id),
        grupoColor: s.curso.codigo,
        dia: h.diaSemana,
        inicio: h.horaInicio,
        fin: h.horaFin,
        titulo: `${s.curso.codigo} · ${s.grupo}`,
        nombre: s.curso.nombre,
        subtitulo: this.mostrarAula() ? `Aula ${h.aula}` : undefined,
        detalle: this.mostrarDocente() ? (s.docenteNombre ?? 'Docente por asignar') : undefined,
      })),
    ),
  );

  protected readonly horasSemanales = computed(
    () => this.bloques().reduce((t, b) => t + (aMinutos(b.fin) - aMinutos(b.inicio)), 0) / 60,
  );

  /** Misma informacion en formato de lista (accesible e imprimible). */
  protected readonly clases = computed<ClaseDelHorario[]>(() =>
    this.seccionesVisibles()
      .flatMap((s) =>
        s.horarios.map((h) => ({
          seccion: s,
          dia: NOMBRE_DIA[h.diaSemana],
          orden: DIAS_SEMANA.indexOf(h.diaSemana) * 1440 + aMinutos(h.horaInicio),
          inicio: h.horaInicio,
          fin: h.horaFin,
          aula: h.aula,
        })),
      )
      .sort((a, b) => a.orden - b.orden),
  );

  ngOnInit(): void {
    const solicitado = Number(this.matriculaId()) || null;
    this.periodoService.listar().subscribe({
      next: (periodos) => {
        this.periodos.set([...periodos].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)));
        if (this.esCoordinacion()) {
          this.iniciarCoordinacion(periodos, solicitado);
        } else {
          this.iniciarEstudiante(periodos, solicitado);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudieron consultar los periodos.'));
        this.loading.set(false);
      },
    });
  }

  private iniciarEstudiante(periodos: Periodo[], solicitado: number | null): void {
    this.matriculaService.buscar({}, 0, 50).subscribe({
      next: (pagina) => {
        const vigentes = pagina.content.filter((m) => m.estado !== ESTADO_ANULADA);
        this.misMatriculas.set(vigentes);
        const activo = periodoActivo(periodos);
        const elegida =
          vigentes.find((m) => m.id === solicitado) ??
          vigentes.find((m) => m.periodoId === activo?.id) ??
          vigentes[0];
        if (elegida) {
          this.mostrar(elegida.id);
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

  private iniciarCoordinacion(periodos: Periodo[], solicitado: number | null): void {
    this.periodoBuscado.set(periodoActivo(periodos)?.id ?? periodos[0]?.id ?? null);
    this.alumnoService.listar().subscribe({
      next: (alumnos) => this.alumnos.set(alumnos),
      error: () => this.error.set('No se pudo cargar la lista de alumnos para la búsqueda.'),
    });
    if (solicitado) {
      this.mostrar(solicitado);
    } else {
      this.loading.set(false);
    }
  }

  /** COORDINACION: matricula vigente del alumno en el periodo elegido. */
  buscar(): void {
    const periodoId = this.periodoBuscado();
    const estudianteId = this.alumnoBuscado();
    if (!periodoId || !estudianteId) return;
    this.buscando.set(true);
    this.error.set(null);
    this.sinResultado.set(null);
    this.matriculaService.buscar({ periodoId, estudianteId }, 0, 20).subscribe({
      next: (pagina) => {
        this.buscando.set(false);
        const vigente = matriculaVigente(pagina.content);
        if (vigente) {
          this.mostrar(vigente.id);
        } else {
          this.matricula.set(null);
          this.sinResultado.set(
            pagina.content.length
              ? 'El alumno solo tiene matrículas anuladas en ese periodo; una matrícula anulada no tiene horario.'
              : 'El alumno no tiene matrícula en ese periodo.',
          );
        }
      },
      error: (err: HttpErrorResponse) => {
        this.buscando.set(false);
        this.error.set(mensajeHttp(err, 'No se pudo buscar la matrícula.'));
      },
    });
  }

  mostrar(id: number | null): void {
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.sinResultado.set(null);
    this.cursoFiltro.set(null);
    this.router.navigate([], { queryParams: { matriculaId: id }, replaceUrl: true });
    this.matriculaService.obtener(id).subscribe({
      next: (matricula) => {
        this.matricula.set(matricula);
        if (this.esCoordinacion()) {
          this.periodoBuscado.set(matricula.periodoId);
          this.alumnoBuscado.set(matricula.estudianteId);
        }
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

  protected codigoPeriodo(id: number): string {
    return this.periodos().find((p) => p.id === id)?.codigo ?? `Periodo ${id}`;
  }

  imprimir(): void {
    window.print();
  }
}
