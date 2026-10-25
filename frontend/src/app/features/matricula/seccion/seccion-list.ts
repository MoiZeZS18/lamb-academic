import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { LucidePencil, LucidePlus, LucideTrash } from '@lucide/angular';
import { AuthService } from '../../../core/auth/auth-service';
import { ROLES_COORDINACION } from '../../../core/auth/roles';
import { AvisoService } from '../../../core/services/aviso-service';
import { mensajeHttp } from '../../../core/services/error-http';
import { confirmar } from '../../../shared/componentes/confirmar-dialogo';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { resumenHorario } from '../../../shared/horario/horario-util';
import { Curso } from '../../core-academico/curso/curso.model';
import { CursoService } from '../../core-academico/curso/curso-service';
import { Periodo } from '../periodo/periodo.model';
import { PeriodoService } from '../periodo/periodo-service';
import { ESTADOS_SECCION, EstadoSeccion, Seccion } from './seccion.model';
import { SeccionService } from './seccion-service';

/**
 * CRUD dependiente (S8): cada seccion depende de un Curso y de un Periodo. La lista muestra el
 * curso por su codigo y nombre (viene anidado en SeccionResponse, sin llamadas extra) y filtra en
 * el servidor por periodo, curso y estado.
 */
@Component({
  selector: 'app-seccion-list',
  imports: [RouterLink, FormsModule, EtiquetaEstado, LucidePencil, LucidePlus, LucideTrash],
  templateUrl: './seccion-list.html',
})
export class SeccionList implements OnInit {
  private readonly seccionService = inject(SeccionService);
  private readonly periodoService = inject(PeriodoService);
  private readonly cursoService = inject(CursoService);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(AvisoService);
  protected readonly auth = inject(AuthService);

  protected readonly estados = ESTADOS_SECCION;
  protected readonly resumenHorario = resumenHorario;
  protected readonly rolesCoordinacion = ROLES_COORDINACION;

  protected readonly secciones = signal<Seccion[]>([]);
  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly cursos = signal<Curso[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  protected periodoFiltro: number | null = null;
  protected cursoFiltro: number | null = null;
  protected estadoFiltro: EstadoSeccion | null = null;
  protected soloConVacantes = false;

  protected readonly puedeGestionar = computed(() => this.auth.tieneRol(...ROLES_COORDINACION));

  ngOnInit(): void {
    this.cursoService.listar().subscribe({
      next: (data) => this.cursos.set(data),
      error: () => this.error.set('No se pudo cargar la lista de cursos para el filtro.'),
    });
    this.periodoService.listar().subscribe({
      next: (data) => {
        this.periodos.set([...data].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)));
        this.periodoFiltro = data.find((p) => p.estado === 'ACTIVO')?.id ?? null;
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.error.set(mensajeHttp(err, 'No se pudieron cargar los periodos.')),
    });
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.seccionService
      .listar({
        periodoId: this.periodoFiltro,
        cursoId: this.cursoFiltro,
        estado: this.estadoFiltro,
        conVacantes: this.soloConVacantes,
      })
      .subscribe({
        next: (data) => this.secciones.set(data),
        error: (err: HttpErrorResponse) => {
          this.error.set(mensajeHttp(err, 'No se pudo cargar la lista de secciones.'));
          this.loading.set(false);
        },
        complete: () => this.loading.set(false),
      });
  }

  protected codigoPeriodo(id: number): string {
    return this.periodos().find((p) => p.id === id)?.codigo ?? `#${id}`;
  }

  protected ocupacion(seccion: Seccion): number {
    return seccion.limiteVacantes ? Math.round((seccion.cuposOcupados / seccion.limiteVacantes) * 100) : 0;
  }

  /**
   * Motivo visible por el que no conviene ofrecer la eliminacion (UX). El backend lo vuelve a
   * validar e incluye un caso que la SPA no ve: detalles de matriculas ya anuladas.
   */
  protected motivoNoEliminable(seccion: Seccion): string | null {
    if (seccion.cuposOcupados > 0) return 'Tiene matriculados: cancélala desde Editar en lugar de eliminarla.';
    if (this.periodos().find((p) => p.id === seccion.periodoId)?.estado === 'CERRADO') {
      return 'Pertenece a un periodo cerrado: se conserva su historia.';
    }
    return null;
  }

  eliminar(seccion: Seccion): void {
    confirmar(this.dialog, {
      titulo: `Eliminar la sección ${seccion.curso.codigo}-${seccion.grupo}`,
      mensaje:
        'La sección y sus horarios se borrarán de forma definitiva y dejarán de aparecer en la oferta. ' +
        'Solo es posible si nunca se usó en una matrícula (ni siquiera anulada); si ya se usó, el sistema lo ' +
        'rechazará y deberás cancelarla.',
      detalle: [
        `Curso: ${seccion.curso.codigo} · ${seccion.curso.nombre}`,
        `Periodo: ${this.codigoPeriodo(seccion.periodoId)}`,
        `Horario: ${resumenHorario(seccion.horarios) || 'sin horario'}`,
      ],
      confirmar: 'Eliminar definitivamente',
      peligro: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.seccionService.eliminar(seccion.id).subscribe({
        next: () => {
          this.avisos.mostrar(`Sección ${seccion.curso.codigo}-${seccion.grupo} eliminada.`, 'ok');
          this.cargar();
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 404) {
            this.error.set('La sección ya no existe. Se recargó la lista.');
            this.cargar();
          } else {
            this.error.set(mensajeHttp(err, 'No se pudo eliminar la sección.'));
          }
        },
      });
    });
  }
}
