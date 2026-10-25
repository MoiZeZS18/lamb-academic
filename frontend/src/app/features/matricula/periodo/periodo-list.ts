import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { RouterLink } from '@angular/router';
import { LucidePencil, LucidePlus, LucideTrash } from '@lucide/angular';
import { AuthService } from '../../../core/auth/auth-service';
import { Rol } from '../../../core/auth/roles';
import { AvisoService } from '../../../core/services/aviso-service';
import { mensajeHttp } from '../../../core/services/error-http';
import { confirmar } from '../../../shared/componentes/confirmar-dialogo';
import { EtiquetaEstado } from '../../../shared/componentes/etiqueta-estado';
import { Periodo } from './periodo.model';
import { PeriodoService } from './periodo-service';

/** CRUD independiente (S7): lista de periodos; crear, editar y eliminar solo para ADMIN. */
@Component({
  selector: 'app-periodo-list',
  imports: [RouterLink, DatePipe, EtiquetaEstado, LucidePencil, LucidePlus, LucideTrash],
  templateUrl: './periodo-list.html',
})
export class PeriodoList implements OnInit {
  private readonly periodoService = inject(PeriodoService);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(AvisoService);
  protected readonly auth = inject(AuthService);
  protected readonly rolAdmin = Rol.ADMIN;

  protected readonly periodos = signal<Periodo[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.periodoService.listar().subscribe({
      next: (data) => this.periodos.set([...data].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio))),
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeHttp(err, 'No se pudo cargar la lista de periodos.'));
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  eliminar(periodo: Periodo): void {
    confirmar(this.dialog, {
      titulo: `Eliminar el periodo ${periodo.codigo}`,
      mensaje:
        'El periodo se borrará de forma definitiva. Solo es posible si no tiene matrículas ni secciones ' +
        'programadas; si las tiene, el sistema lo rechazará y el periodo se conservará.',
      detalle: [`${periodo.nombre}`, `Estado: ${periodo.estado}`],
      confirmar: 'Eliminar definitivamente',
      peligro: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.periodoService.eliminar(periodo.id).subscribe({
        next: () => {
          this.avisos.mostrar(`Periodo ${periodo.codigo} eliminado.`, 'ok');
          this.cargar();
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 404) {
            this.error.set('El periodo ya no existe. Se recargó la lista.');
            this.cargar();
          } else if (err.status === 409 && err.error?.message?.startsWith('La operacion viola')) {
            this.error.set(`No se puede eliminar ${periodo.codigo}: tiene secciones programadas.`);
          } else {
            this.error.set(mensajeHttp(err, 'No se pudo eliminar el periodo.'));
          }
        },
      });
    });
  }
}
