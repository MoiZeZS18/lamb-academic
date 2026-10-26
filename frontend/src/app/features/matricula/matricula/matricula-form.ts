import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, FormsModule, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';
import { LucideCircleAlert, LucidePlus, LucideSearch, LucideX } from '@lucide/angular';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION, Rol } from '../../../core/auth/roles';
import { AvisoService } from '../../../core/services/aviso-service';
import { cuerpoError, mensajeHttp } from '../../../core/services/error-http';
import { confirmar } from '../../../shared/componentes/confirmar-dialogo';
import { EstadoVacio } from '../../../shared/componentes/estado-vacio';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { BloqueSemanal, HorarioSemanal } from '../../../shared/horario/horario-semanal';
import { buscarCruces, resumenHorario, seCruzan } from '../../../shared/horario/horario-util';
import { alMenosUnaLinea, sinRepetidos } from '../../../shared/validadores';
import { Alumno, AlumnoService } from '../../base-universal/alumno/alumno-service';
import { CursoResumen } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { DetallePlan, PlanEstudio } from '../../core-academico/plan/plan-estudio.model';
import { PlanEstudioService } from '../../core-academico/plan/plan-estudio-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { Seccion } from '../seccion/seccion.model';
import { SeccionService } from '../seccion/seccion-service';
import { hoyIso, matriculaVigente, motivoBloqueo, periodoActivo } from './contexto-matricula';
import { ContextoAcademico, ESTADO_INICIAL_MATRICULA, Matricula } from './matricula.model';
import { MatriculaService } from './matricula-service';

interface LineaValor {
  cursoId: number;
  seccionId: number;
}

/**
 * Inscripcion (pantallas B y C, S9): cabecera con el alumno (el del token para ESTUDIANTE) y un
 * FormArray de lineas {curso, seccion}. Las validaciones de la SPA son una ayuda: el backend vuelve
 * a validar todo y genera el cargo en la misma transaccion, con UNA sola peticion.
 */
@Component({
  selector: 'app-matricula-form',
  imports: [
    ReactiveFormsModule,
    FormsModule,
    RouterLink,
    EtiquetaEstado,
    EstadoVacio,
    HorarioSemanal,
    LucideCircleAlert,
    LucidePlus,
    LucideSearch,
    LucideX,
  ],
  templateUrl: './matricula-form.html',
  styleUrl: './matricula-form.css',
})
export class MatriculaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly periodoService = inject(PeriodoService);
  private readonly seccionService = inject(SeccionService);
  private readonly cursoService = inject(CursoService);
  private readonly planService = inject(PlanEstudioService);
  private readonly alumnoService = inject(AlumnoService);
  private readonly matriculaService = inject(MatriculaService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisoService);
  protected readonly auth = inject(AuthService);

  protected readonly resumenHorario = resumenHorario;
  protected readonly cuotas = Array.from({ length: 12 }, (_, i) => i + 1);

  protected readonly periodo = signal<Periodo | null>(null);
  protected readonly secciones = signal<Seccion[]>([]);
  protected readonly alumnos = signal<Alumno[]>([]);
  protected readonly prerrequisitos = signal<Map<number, CursoResumen[]>>(new Map());
  protected readonly existente = signal<Matricula | null>(null);
  /** Escuela y habilitacion del alumno elegido (GET /matriculas/contexto). */
  protected readonly contexto = signal<ContextoAcademico | null>(null);
  /** Plan vigente de la escuela del alumno (Core Academico); null si la escuela no tiene uno. */
  protected readonly plan = signal<PlanEstudio | null>(null);
  protected readonly cicloFiltro = signal<number | null>(null);
  protected readonly cargando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errorCarga = signal<string | null>(null);

  protected readonly busqueda = signal('');
  protected readonly soloDisponibles = signal(true);
  protected readonly busquedaAlumno = signal('');

  /** ESTUDIANTE sin rol de personal: matricula solo a si mismo (el backend lo exige igual). */
  protected readonly esEstudiante = computed(
    () => this.auth.tieneRol(Rol.ESTUDIANTE) && !this.auth.tieneRol(...ROLES_COORDINACION),
  );

  protected readonly form = this.fb.nonNullable.group({
    estudianteId: [0, [Validators.min(1)]],
    numeroCuotas: [5, [Validators.required, Validators.min(1), Validators.max(12)]],
    detalles: this.fb.array([this.crearLinea()], [alMenosUnaLinea, sinRepetidos('cursoId')]),
  });

  protected readonly valoresDetalles = toSignal(this.form.controls.detalles.valueChanges, {
    initialValue: this.form.controls.detalles.getRawValue(),
  });

  protected readonly alumnoElegido = toSignal(this.form.controls.estudianteId.valueChanges, {
    initialValue: this.form.controls.estudianteId.value,
  });

  /** Alumnos que coinciden con la busqueda; el elegido se conserva aunque no coincida. */
  protected readonly alumnosFiltrados = computed(() => {
    const texto = this.busquedaAlumno().trim().toLowerCase();
    const elegido = this.alumnoElegido();
    if (!texto) return this.alumnos();
    return this.alumnos().filter(
      (a) =>
        a.id === elegido ||
        `${a.codigoEstudiante} ${a.persona.nombreCompleto} ${a.persona.numeroDocumento}`.toLowerCase().includes(texto),
    );
  });

  private readonly seccionPorId = computed(() => new Map(this.secciones().map((s) => [s.id, s])));

  protected readonly cursosOfertados = computed(() => {
    const cursos = new Map<number, CursoResumen>();
    for (const s of this.secciones()) cursos.set(s.curso.id, s.curso);
    return [...cursos.values()].sort((a, b) => a.codigo.localeCompare(b.codigo));
  });

  protected readonly planPorCurso = computed(
    () => new Map((this.plan()?.detalles ?? []).map((d): [number, DetallePlan] => [d.idCurso, d])),
  );

  protected readonly ciclosOfertados = computed(() =>
    [...new Set(this.secciones().map((s) => this.planPorCurso().get(s.curso.id)?.ciclo))]
      .filter((c): c is number => c !== undefined)
      .sort((a, b) => a - b),
  );

  protected readonly ofertaFiltrada = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const ciclo = this.cicloFiltro();
    const enPlan = this.planPorCurso();
    // Con plan: primero por ciclo (los cursos fuera del plan al final); luego por codigo y grupo.
    const orden = (s: Seccion) => enPlan.get(s.curso.id)?.ciclo ?? Number.MAX_SAFE_INTEGER;
    return this.secciones()
      .filter((s) => !this.soloDisponibles() || this.disponible(s))
      .filter((s) => ciclo === null || enPlan.get(s.curso.id)?.ciclo === ciclo)
      .filter((s) => !texto || `${s.curso.codigo} ${s.curso.nombre} ${s.docenteNombre ?? ''}`.toLowerCase().includes(texto))
      .sort((a, b) => orden(a) - orden(b) || a.curso.codigo.localeCompare(b.curso.codigo) || a.grupo.localeCompare(b.grupo));
  });

  protected readonly elegidas = computed(() =>
    (this.valoresDetalles() as LineaValor[])
      .map((l) => this.seccionPorId().get(l.seccionId))
      .filter((s): s is Seccion => !!s),
  );

  protected readonly totalCreditos = computed(() => this.elegidas().reduce((t, s) => t + s.curso.creditos, 0));

  protected readonly cruces = computed(() =>
    buscarCruces(
      this.elegidas().map((s) => ({ clave: String(s.id), etiqueta: `${s.curso.codigo}-${s.grupo}`, horarios: s.horarios })),
    ),
  );

  /** Solo se marcan en conflicto los bloques que realmente se solapan con otra seccion. */
  protected readonly bloquesHorario = computed<BloqueSemanal[]>(() => {
    const elegidas = this.elegidas();
    return elegidas.flatMap((s) =>
      s.horarios.map((h) => ({
        clave: String(s.id),
        dia: h.diaSemana,
        inicio: h.horaInicio,
        fin: h.horaFin,
        titulo: `${s.curso.codigo}-${s.grupo}`,
        subtitulo: h.aula,
        conflicto: elegidas.some((otra) => otra.id !== s.id && otra.horarios.some((o) => seCruzan(h, o))),
      })),
    );
  });

  protected readonly bloqueo = computed(() => motivoBloqueo(this.periodo()));

  /** Base Universal indica que el alumno no esta habilitado: el backend rechazaria el registro. */
  protected readonly noHabilitado = computed(() => {
    const c = this.contexto();
    return c !== null && (!c.existe || !c.habilitado);
  });

  /**
   * Regla visible del backend (PrerrequisitoNoCumplido): un curso y su prerrequisito no pueden
   * llevarse en el mismo periodo. Se avisa antes de enviar; el historial lo verifica el backend.
   */
  protected readonly prerrequisitosEnLaMisma = computed(() => {
    const elegidos = new Map(this.elegidas().map((s) => [s.curso.id, s.curso]));
    return this.elegidas().flatMap((s) =>
      this.prerrequisitosDe(s.curso.id)
        .filter((pre) => elegidos.has(pre.id))
        .map((pre) => `${s.curso.codigo} requiere haber aprobado ${pre.codigo}; no pueden llevarse en el mismo periodo.`),
    );
  });

  /** true si algun curso elegido declara prerrequisitos en la malla (informativo; decide el backend). */
  protected readonly prerrequisitosPendientes = computed(() =>
    (this.valoresDetalles() as LineaValor[]).some((l) => (this.prerrequisitos().get(l.cursoId) ?? []).length > 0),
  );

  ngOnInit(): void {
    if (this.esEstudiante()) {
      const alumnoId = this.auth.alumnoId();
      if (alumnoId === null) {
        this.errorCarga.set('Tu cuenta no está vinculada a ningún alumno; no puedes registrar una matrícula.');
        this.cargando.set(false);
        return;
      }
      this.form.controls.estudianteId.setValue(alumnoId);
      this.cargarContexto(null);
    }
    this.periodoService.listar().subscribe({
      next: (periodos) => {
        const activo = periodoActivo(periodos);
        this.periodo.set(activo);
        if (!activo) {
          this.cargando.set(false);
          return;
        }
        forkJoin({
          secciones: this.seccionService.listar({ periodoId: activo.id }),
          alumnos: this.esEstudiante() ? of([]) : this.alumnoService.listar('ACTIVO'),
        }).subscribe({
          next: ({ secciones, alumnos }) => {
            this.secciones.set(secciones);
            this.alumnos.set(alumnos);
            this.cargando.set(false);
            if (this.esEstudiante()) this.verificarExistente();
          },
          error: (err: HttpErrorResponse) => {
            this.errorCarga.set(mensajeHttp(err, 'No se pudo cargar la oferta del periodo.'));
            this.cargando.set(false);
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.errorCarga.set(mensajeHttp(err, 'No se pudo consultar el periodo académico.'));
        this.cargando.set(false);
      },
    });
  }

  protected get lineasForm() {
    return this.form.controls.detalles;
  }

  private crearLinea(cursoId = 0, seccionId = 0) {
    return this.fb.nonNullable.group(
      {
        cursoId: [cursoId, [Validators.min(1)]],
        seccionId: [seccionId, [Validators.min(1)]],
      },
      { validators: (linea: AbstractControl) => this.validarLinea(linea) },
    );
  }

  /** La seccion elegida debe ser del curso de la linea, estar ABIERTA y tener vacantes. */
  private validarLinea(linea: AbstractControl): ValidationErrors | null {
    const { cursoId, seccionId } = linea.value as LineaValor;
    const seccion = this.seccionPorId?.().get(seccionId);
    if (!seccion) return null;
    if (seccion.curso.id !== cursoId) return { otroCurso: true };
    if (seccion.estado !== 'ABIERTA') return { noAbierta: seccion.estado };
    if (seccion.vacantesDisponibles <= 0) return { sinVacantes: true };
    return null;
  }

  protected disponible(seccion: Seccion): boolean {
    return seccion.estado === 'ABIERTA' && seccion.vacantesDisponibles > 0;
  }

  protected seccionesDelCurso(cursoId: number): Seccion[] {
    return this.secciones().filter((s) => s.curso.id === cursoId);
  }

  protected seccion(id: number): Seccion | undefined {
    return this.seccionPorId().get(id);
  }

  protected indiceDeSeccion(seccionId: number): number {
    return (this.valoresDetalles() as LineaValor[]).findIndex((l) => l.seccionId === seccionId);
  }

  /** Desde la oferta: si el curso ya esta en una linea, se cambia su seccion; si no, se agrega. */
  protected elegirSeccion(seccion: Seccion): void {
    const lineas = this.lineasForm.controls;
    const existente = lineas.find((l) => l.controls.cursoId.value === seccion.curso.id);
    const vacia = lineas.find((l) => !l.controls.cursoId.value);
    const destino = existente ?? vacia;
    if (destino) {
      destino.setValue({ cursoId: seccion.curso.id, seccionId: seccion.id });
      destino.markAsTouched();
    } else {
      this.lineasForm.push(this.crearLinea(seccion.curso.id, seccion.id));
    }
    this.cargarPrerrequisitos(seccion.curso.id);
  }

  protected agregarLinea(): void {
    this.lineasForm.push(this.crearLinea());
  }

  protected quitarLinea(indice: number): void {
    this.lineasForm.removeAt(indice);
  }

  /** Al cambiar el curso de una linea, la seccion vuelve a "sin elegir" (dependencia S8). */
  protected cambiarCurso(indice: number): void {
    const linea = this.lineasForm.at(indice);
    const cursoId = linea.controls.cursoId.value;
    const secciones = this.seccionesDelCurso(cursoId).filter((s) => this.disponible(s));
    linea.controls.seccionId.setValue(secciones.length === 1 ? secciones[0].id : 0);
    this.cargarPrerrequisitos(cursoId);
  }

  /**
   * Escuela del alumno y su plan vigente. Los prerrequisitos dependen de la escuela, asi que al
   * cambiar de alumno se vuelven a consultar los de los cursos ya elegidos.
   */
  private cargarContexto(estudianteId: number | null): void {
    this.contexto.set(null);
    this.plan.set(null);
    this.cicloFiltro.set(null);
    this.prerrequisitos.set(new Map());
    this.matriculaService.contexto(estudianteId).subscribe({
      next: (contexto) => {
        this.contexto.set(contexto);
        const cursosElegidos = (this.valoresDetalles() as LineaValor[]).map((l) => l.cursoId);
        cursosElegidos.forEach((cursoId) => this.cargarPrerrequisitos(cursoId));
        if (!contexto.escuelaId) return;
        this.planService.buscar(contexto.escuelaId, 'VIGENTE').subscribe({
          next: (planes) => this.plan.set(planes[0] ?? null),
          error: () => this.plan.set(null),
        });
      },
      error: () => this.contexto.set(null),
    });
  }

  /** Solo con la escuela conocida: sin ella, el backend mezclaria los planes de todas las escuelas. */
  private cargarPrerrequisitos(cursoId: number): void {
    const escuelaId = this.contexto()?.escuelaId;
    if (!cursoId || !escuelaId || this.prerrequisitos().has(cursoId)) return;
    const guardar = (lista: CursoResumen[]) => {
      // Si mientras tanto cambio el alumno (y su escuela), esta respuesta ya no aplica.
      if (this.contexto()?.escuelaId === escuelaId) {
        this.prerrequisitos.update((m) => new Map(m).set(cursoId, lista));
      }
    };
    this.cursoService.prerrequisitos(cursoId, escuelaId).subscribe({
      next: guardar,
      error: () => guardar([]),
    });
  }

  protected prerrequisitosDe(cursoId: number): CursoResumen[] {
    return this.prerrequisitos().get(cursoId) ?? [];
  }

  protected cambiarAlumno(): void {
    this.existente.set(null);
    const estudianteId = this.form.controls.estudianteId.value;
    if (estudianteId) {
      this.verificarExistente();
      this.cargarContexto(estudianteId);
    }
  }

  private verificarExistente(): void {
    const periodo = this.periodo();
    const estudianteId = this.form.controls.estudianteId.value;
    if (!periodo || !estudianteId) return;
    const filtros = this.esEstudiante() ? { periodoId: periodo.id } : { periodoId: periodo.id, estudianteId };
    this.matriculaService.buscar(filtros, 0, 20).subscribe({
      next: (pagina) => this.existente.set(matriculaVigente(pagina.content)),
      error: () => this.existente.set(null),
    });
  }

  /** Mensajes que impiden registrar, para el panel de resumen (sin repetir las validaciones). */
  protected readonly bloqueosEnvio = computed(() => {
    const mensajes: string[] = [];
    if (!this.alumnoElegido()) mensajes.push('Elige el alumno.');
    if (this.noHabilitado()) mensajes.push('El alumno no está habilitado para matricularse (Base Universal).');
    if (this.existente()) mensajes.push('El alumno ya tiene una matrícula vigente en el periodo.');
    if (!this.elegidas().length) mensajes.push('Elige al menos un curso.');
    if (this.cruces().length) mensajes.push(`${this.cruces().length} cruce(s) de horario.`);
    if (this.prerrequisitosEnLaMisma().length) mensajes.push('Un curso y su prerrequisito en la misma matrícula.');
    return mensajes;
  });

  protected nombreAlumno(): string {
    if (this.esEstudiante()) return this.auth.usuario()?.nombre ?? '';
    const alumno = this.alumnos().find((a) => a.id === this.alumnoElegido());
    return alumno ? `${alumno.codigoEstudiante} · ${alumno.persona.nombreCompleto}` : '';
  }

  protected confirmarYRegistrar(): void {
    if (this.enviando()) return;
    this.error.set(null);
    if (this.form.invalid || this.cruces().length > 0 || this.prerrequisitosEnLaMisma().length > 0) {
      this.form.markAllAsTouched();
      this.lineasForm.controls.forEach((l) => l.markAllAsTouched());
      this.error.set(
        this.cruces().length
          ? 'Hay cruces de horario entre las secciones elegidas. Cambia una de ellas antes de registrar.'
          : this.prerrequisitosEnLaMisma().length
            ? this.prerrequisitosEnLaMisma()[0]
            : 'Revisa la matrícula: hay datos pendientes o no válidos.',
      );
      return;
    }
    const periodo = this.periodo()!;
    const cuotas = this.form.controls.numeroCuotas.value;
    confirmar(this.dialog, {
      titulo: 'Confirmar matrícula',
      mensaje:
        `Se registrará la matrícula de ${this.nombreAlumno()} en el periodo ${periodo.codigo} con ` +
        `${this.elegidas().length} curso(s) y ${this.totalCreditos()} créditos. El cargo financiero se genera ` +
        `automáticamente en ${cuotas} cuota(s).`,
      detalle: this.elegidas().map(
        (s) => `${s.curso.codigo} ${s.curso.nombre} · grupo ${s.grupo} · ${s.curso.creditos} cr. · ${resumenHorario(s.horarios)}`,
      ),
      confirmar: 'Registrar matrícula',
    }).subscribe((ok) => {
      if (ok) this.registrar();
    });
  }

  private registrar(): void {
    const periodo = this.periodo()!;
    const valor = this.form.getRawValue();
    this.enviando.set(true);
    this.matriculaService
      .registrar({
        matricula: {
          estudianteId: valor.estudianteId,
          periodoId: periodo.id,
          fecha: hoyIso(),
          estado: ESTADO_INICIAL_MATRICULA,
          numeroCuotas: valor.numeroCuotas,
        },
        detalles: valor.detalles.map((l) => ({ seccionId: l.seccionId })),
      })
      .subscribe({
        next: (matricula) => {
          this.avisos.mostrar(
            `Matrícula ${matricula.id} registrada` +
              (matricula.cargo ? ` con el cargo ${matricula.cargo.numeroCargo}.` : '.'),
            'ok',
          );
          this.router.navigate(['/matricula/consulta', matricula.id]);
        },
        error: (err: HttpErrorResponse) => this.manejarErrorRegistro(err),
      });
  }

  /** Errores reales del backend con su causa (S9 3.9); los de cupos refrescan la oferta. */
  private manejarErrorRegistro(err: HttpErrorResponse): void {
    const mensaje = cuerpoError(err)?.message ?? '';
    if (err.status === 409 && /cupos|vacantes|abierta|no pertenece/i.test(mensaje)) {
      this.error.set(`${mensaje}. Se actualizó la oferta con las vacantes actuales.`);
      this.recargarOferta();
    } else if (err.status === 404 && /secci/i.test(mensaje)) {
      this.error.set(`${mensaje}. Se actualizó la oferta del periodo.`);
      this.recargarOferta();
    } else if (err.status === 409 && /matricula activa/i.test(mensaje)) {
      this.error.set(mensaje);
      this.verificarExistente();
    } else {
      this.error.set(mensajeHttp(err, 'No se pudo registrar la matrícula.'));
    }
    this.enviando.set(false);
  }

  private recargarOferta(): void {
    const periodo = this.periodo();
    if (!periodo) return;
    this.seccionService.listar({ periodoId: periodo.id }).subscribe({
      next: (secciones) => {
        this.secciones.set(secciones);
        this.lineasForm.controls.forEach((l) => l.updateValueAndValidity());
      },
    });
  }

  protected errorLinea(indice: number): string {
    const linea = this.lineasForm.at(indice);
    if (!linea.touched && !linea.dirty) return '';
    if (linea.controls.cursoId.hasError('min')) return 'Selecciona un curso.';
    if (linea.controls.seccionId.hasError('min')) return 'Selecciona una sección del curso.';
    if (linea.hasError('noAbierta')) return `La sección está ${String(linea.getError('noAbierta')).toLowerCase()}.`;
    if (linea.hasError('sinVacantes')) return 'La sección no tiene vacantes.';
    if (linea.hasError('otroCurso')) return 'La sección no corresponde al curso.';
    return '';
  }
}
