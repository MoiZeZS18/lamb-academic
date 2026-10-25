import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucidePlus, LucideX } from '@lucide/angular';
import { AuthService } from '../../../core/auth/auth-service';
import { Rol } from '../../../core/auth/roles';
import { AvisoService } from '../../../core/services/aviso-service';
import { cuerpoError, mensajeHttp } from '../../../core/services/error-http';
import { confirmar } from '../../../shared/componentes/confirmar-dialogo';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { HORA_MAXIMA, HORA_MINIMA, NOMBRE_DIA, aMinutos, seCruzan } from '../../../shared/horario/horario-util';
import { entero } from '../../../shared/validadores';
import { Curso } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { Docente, DocenteService } from '../../core-academico/docente/docente-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { DIAS_SEMANA, DiaSemana, EstadoSeccion, HorarioRequest, Seccion } from './seccion.model';
import { SeccionService } from './seccion-service';

/** Una linea de horario: fin posterior al inicio y dentro de la franja 07:00-22:00 del backend. */
function horarioValido(linea: AbstractControl): ValidationErrors | null {
  const { horaInicio, horaFin } = linea.value as HorarioRequest;
  if (!horaInicio || !horaFin) return null;
  if (aMinutos(horaFin) <= aMinutos(horaInicio)) return { horaFin: true };
  if (aMinutos(horaInicio) < HORA_MINIMA * 60 || aMinutos(horaFin) > HORA_MAXIMA * 60) return { franja: true };
  return null;
}

/** Los horarios de la misma seccion no pueden cruzarse entre si (misma regla que SeccionService). */
function horariosSinCruce(control: AbstractControl): ValidationErrors | null {
  const lineas = (control as FormArray).getRawValue() as HorarioRequest[];
  for (let i = 0; i < lineas.length; i++) {
    for (let j = i + 1; j < lineas.length; j++) {
      if (lineas[i].horaInicio && lineas[j].horaInicio && seCruzan(lineas[i], lineas[j])) {
        return { cruce: `${NOMBRE_DIA[lineas[i].diaSemana]}: las líneas ${i + 1} y ${j + 1} se cruzan` };
      }
    }
  }
  return null;
}

/**
 * Formulario del CRUD dependiente (S8). Al crear: curso (autocomplete, S8 3.10), periodo y docente
 * desde sus servicios, mas los horarios en un FormArray. Al editar, el contrato del backend solo
 * permite cambiar docente y vacantes (SeccionActualizacionRequest), y el estado por su endpoint.
 */
@Component({
  selector: 'app-seccion-form',
  imports: [ReactiveFormsModule, RouterLink, MatAutocompleteModule, EtiquetaEstado, LucidePlus, LucideX],
  templateUrl: './seccion-form.html',
  styleUrl: './seccion-form.css',
})
export class SeccionForm {
  private readonly fb = inject(FormBuilder);
  private readonly seccionService = inject(SeccionService);
  private readonly cursoService = inject(CursoService);
  private readonly periodoService = inject(PeriodoService);
  private readonly docenteService = inject(DocenteService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(AvisoService);
  protected readonly auth = inject(AuthService);

  protected readonly dias = DIAS_SEMANA;
  protected readonly nombreDia = NOMBRE_DIA;
  protected readonly rolAdmin = Rol.ADMIN;

  protected readonly id = signal<number | null>(null);
  protected readonly seccion = signal<Seccion | null>(null);
  protected readonly cursos = signal<Curso[]>([]);
  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly docentes = signal<Docente[]>([]);
  protected readonly cursosCargados = signal(false);
  protected readonly periodosCargados = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly cursoBusqueda = signal('');
  protected readonly cursosFiltrados = computed(() => {
    const texto = this.cursoBusqueda().trim().toLowerCase();
    const activos = this.cursos().filter((c) => c.activo);
    if (!texto) return activos;
    return activos.filter((c) => `${c.codigo} ${c.nombre}`.toLowerCase().includes(texto));
  });

  /** Solo periodos donde se puede programar: el backend rechaza los cerrados. */
  protected readonly periodosAbiertos = computed(() => this.periodos().filter((p) => p.estado !== 'CERRADO'));

  protected readonly form = this.fb.nonNullable.group({
    cursoId: [0, [Validators.min(1)]],
    periodoId: [0, [Validators.min(1)]],
    grupo: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{1,10}$/)]],
    docenteId: [null as number | null],
    limiteVacantes: [30, [Validators.required, Validators.min(1), Validators.max(200), entero]],
    horarios: this.fb.array([this.crearHorario()], [horariosSinCruce]),
  });

  constructor() {
    this.cargarCursos();
    this.cargarPeriodos();
    this.docenteService.listar().subscribe({
      next: (data) => this.docentes.set(data),
      error: () => this.error.set('No se pudo cargar la lista de docentes.'),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.loading.set(true);
      // El contrato de actualizacion no cambia curso, periodo, grupo ni horarios.
      for (const campo of ['cursoId', 'periodoId', 'grupo', 'horarios'] as const) {
        this.form.controls[campo].disable();
      }
      this.seccionService.obtener(id).subscribe({
        next: (seccion) => {
          this.seccion.set(seccion);
          this.form.patchValue({
            cursoId: seccion.curso.id,
            periodoId: seccion.periodoId,
            grupo: seccion.grupo,
            docenteId: seccion.docenteId,
            limiteVacantes: seccion.limiteVacantes,
          });
          this.cursoBusqueda.set(`${seccion.curso.codigo} · ${seccion.curso.nombre}`);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.errorCarga.set(true);
          this.error.set(mensajeHttp(err, 'No se pudo cargar la sección.'));
          this.loading.set(false);
        },
      });
    }
  }

  protected get horariosForm() {
    return this.form.controls.horarios;
  }

  private crearHorario(dia: DiaSemana = 'LUNES') {
    return this.fb.nonNullable.group(
      {
        diaSemana: [dia, [Validators.required]],
        horaInicio: ['08:00', [Validators.required]],
        horaFin: ['10:00', [Validators.required]],
        aula: ['', [Validators.required, Validators.maxLength(50)]],
      },
      { validators: horarioValido },
    );
  }

  protected agregarHorario(): void {
    this.horariosForm.push(this.crearHorario());
  }

  protected quitarHorario(indice: number): void {
    this.horariosForm.removeAt(indice);
  }

  private cargarCursos(): void {
    this.cursoService.listar().subscribe({
      next: (data) => {
        this.cursos.set(data);
        this.cursosCargados.set(true);
      },
      error: () => this.error.set('No se pudieron cargar los cursos.'),
    });
  }

  private cargarPeriodos(): void {
    this.periodoService.listar().subscribe({
      next: (data) => {
        this.periodos.set(data);
        this.periodosCargados.set(true);
        if (!this.id() && !this.form.controls.periodoId.value) {
          const activo = data.find((p) => p.estado === 'ACTIVO');
          if (activo) this.form.controls.periodoId.setValue(activo.id);
        }
      },
      error: () => this.error.set('No se pudieron cargar los periodos.'),
    });
  }

  /** Escribir no basta para elegir: el id del curso solo se fija al seleccionar una opcion (S8 2.6). */
  protected buscarCurso(texto: string): void {
    this.cursoBusqueda.set(texto);
    this.form.controls.cursoId.setValue(0);
  }

  protected onCursoSeleccionado(evento: MatAutocompleteSelectedEvent): void {
    const curso = this.cursos().find((c) => this.etiquetaCurso(c) === evento.option.value);
    this.form.controls.cursoId.setValue(curso?.id ?? 0);
    this.form.controls.cursoId.markAsTouched();
    this.cursoBusqueda.set(evento.option.value);
  }

  /** Texto de la opcion (el codigo del curso es unico, asi que el texto tambien). */
  protected etiquetaCurso(curso: Curso): string {
    return `${curso.codigo} · ${curso.nombre}`;
  }

  guardar(): void {
    if (this.loading() || this.errorCarga()) return;
    this.error.set(null);
    const grupo = this.form.controls.grupo;
    grupo.setValue(grupo.value.trim().toUpperCase());
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const id = this.id();
    const peticion = id
      ? this.seccionService.actualizar(id, { docenteId: valor.docenteId, limiteVacantes: valor.limiteVacantes })
      : this.seccionService.crear({ ...valor, horarios: valor.horarios.map((h) => ({ ...h, aula: h.aula.trim() })) });
    this.loading.set(true);
    peticion.subscribe({
      next: (seccion) => {
        this.avisos.mostrar(`Sección ${seccion.curso.codigo}-${seccion.grupo} ${id ? 'actualizada' : 'programada'}.`, 'ok');
        this.router.navigate(['/matricula/secciones']);
      },
      error: (err: HttpErrorResponse) => this.manejarErrorGuardado(err),
    });
  }

  /** Distingue la dependencia rota (404 de curso/periodo/docente) del resto de errores (S8 3.8). */
  private manejarErrorGuardado(err: HttpErrorResponse): void {
    const mensaje = cuerpoError(err)?.message ?? '';
    if (err.status === 404 && mensaje.startsWith('Curso')) {
      this.error.set('El curso seleccionado ya no existe. Elige otro de la lista.');
      this.form.controls.cursoId.setValue(0);
      this.cursoBusqueda.set('');
      this.cargarCursos();
    } else if (err.status === 404 && mensaje.startsWith('Periodo')) {
      this.error.set('El periodo seleccionado ya no existe. Elige otro de la lista.');
      this.form.controls.periodoId.setValue(0);
      this.cargarPeriodos();
    } else if (err.status === 404 && mensaje.toLowerCase().includes('docente')) {
      this.error.set('El docente seleccionado ya no está disponible. Elige otro.');
      this.form.controls.docenteId.setValue(null);
    } else {
      this.error.set(mensajeHttp(err, 'No se pudo guardar la sección.'));
    }
    this.loading.set(false);
  }

  cambiarEstado(estado: EstadoSeccion): void {
    const seccion = this.seccion();
    if (!seccion) return;
    const textos: Record<EstadoSeccion, string> = {
      ABIERTA: 'Se reabrirá la inscripción en esta sección.',
      CERRADA: 'La sección dejará de aceptar matrículas nuevas; las existentes se conservan.',
      CANCELADA: 'La sección quedará cancelada de forma definitiva. Solo es posible si no tiene matriculados.',
    };
    confirmar(this.dialog, {
      titulo: `Cambiar estado a ${estado}`,
      mensaje: textos[estado],
      confirmar: 'Cambiar estado',
      peligro: estado === 'CANCELADA',
    }).subscribe((ok) => {
      if (!ok) return;
      this.seccionService.cambiarEstado(seccion.id, estado).subscribe({
        next: (actualizada) => {
          this.seccion.set(actualizada);
          this.avisos.mostrar(`La sección quedó ${actualizada.estado}.`, 'ok');
        },
        error: (err: HttpErrorResponse) => this.error.set(mensajeHttp(err, 'No se pudo cambiar el estado.')),
      });
    });
  }

  cancelar(): void {
    this.router.navigate(['/matricula/secciones']);
  }

  protected mensajeValidacion(campo: 'cursoId' | 'periodoId' | 'grupo' | 'limiteVacantes'): string {
    const control = this.form.controls[campo];
    if (!control.touched || control.disabled) return '';
    if (control.hasError('min')) {
      if (campo === 'cursoId') return 'Selecciona un curso de la lista.';
      if (campo === 'periodoId') return 'Selecciona un periodo.';
      return 'Debe ser al menos 1.';
    }
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('pattern')) return 'Solo mayúsculas y números, de 1 a 10 caracteres (p. ej. A, B2).';
    if (control.hasError('max')) return 'Máximo 200 vacantes.';
    if (control.hasError('entero')) return 'Debe ser un número entero.';
    return '';
  }

  protected errorHorario(indice: number): string {
    const linea = this.horariosForm.at(indice);
    if (!linea.touched) return '';
    if (linea.controls.aula.hasError('required')) return 'Indica el aula.';
    if (linea.hasError('horaFin')) return 'La hora de fin debe ser posterior a la de inicio.';
    if (linea.hasError('franja')) return `El horario debe estar entre ${HORA_MINIMA}:00 y ${HORA_MAXIMA}:00.`;
    return '';
  }
}
