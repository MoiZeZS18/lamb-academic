import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AvisoService } from '../../../core/services/aviso-service';
import { mensajeHttp } from '../../../core/services/error-http';
import { rangoFechas } from '../../../shared/validadores';
import { ESTADOS_PERIODO, EstadoPeriodo } from './periodo.model';
import { PeriodoService } from './periodo-service';

/** Formulario de periodo (S7): crear y editar con el mismo componente. Las reglas del backend mandan. */
@Component({
  selector: 'app-periodo-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './periodo-form.html',
})
export class PeriodoForm {
  private readonly fb = inject(FormBuilder);
  private readonly periodoService = inject(PeriodoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisoService);

  protected readonly estados = ESTADOS_PERIODO;
  protected readonly id = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      codigo: ['', [Validators.required, Validators.maxLength(20)]],
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      fechaInicio: ['', [Validators.required]],
      fechaFin: ['', [Validators.required]],
      estado: ['PLANIFICADO' as EstadoPeriodo, [Validators.required]],
    },
    { validators: rangoFechas('fechaInicio', 'fechaFin') },
  );

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.loading.set(true);
      this.periodoService.obtener(id).subscribe({
        next: (periodo) => {
          this.form.patchValue(periodo);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.errorCarga.set(true);
          this.error.set(mensajeHttp(err, 'No se pudo cargar el periodo.'));
          this.loading.set(false);
        },
      });
    }
  }

  guardar(): void {
    if (this.loading() || this.errorCarga()) return;
    this.error.set(null);
    for (const campo of ['codigo', 'nombre'] as const) {
      const control = this.form.controls[campo];
      control.setValue(control.value.trim());
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const id = this.id();
    const peticion = id ? this.periodoService.actualizar(id, valor) : this.periodoService.crear(valor);
    this.loading.set(true);
    peticion.subscribe({
      next: (periodo) => {
        this.avisos.mostrar(`Periodo ${periodo.codigo} ${id ? 'actualizado' : 'registrado'}.`, 'ok');
        this.router.navigate(['/matricula/periodos']);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudo guardar el periodo.'));
        this.loading.set(false);
      },
    });
  }

  cancelar(): void {
    this.router.navigate(['/matricula/periodos']);
  }

  protected mensajeValidacion(campo: 'codigo' | 'nombre' | 'fechaInicio' | 'fechaFin'): string {
    const control = this.form.controls[campo];
    if (!control.touched) return '';
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) {
      return `Máximo ${control.getError('maxlength').requiredLength} caracteres.`;
    }
    if (campo === 'fechaFin' && this.form.hasError('rangoFechas')) {
      return 'La fecha de fin debe ser posterior a la fecha de inicio.';
    }
    return '';
  }
}
