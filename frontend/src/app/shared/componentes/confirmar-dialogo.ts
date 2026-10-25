import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  /** Lineas de resumen (p. ej. cursos y creditos de la matricula). */
  detalle?: string[];
  confirmar: string;
  peligro?: boolean;
}

/**
 * Confirmacion explicita antes de una operacion que cambia datos (S9 3.8). Reemplaza el confirm()
 * del navegador de la guia por un dialogo de Angular Material con el resumen de la operacion.
 */
@Component({
  selector: 'app-confirmar-dialogo',
  imports: [MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ datos.mensaje }}</p>
      @if (datos.detalle?.length) {
        <ul class="detalle">
          @for (linea of datos.detalle; track $index) {
            <li>{{ linea }}</li>
          }
        </ul>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button type="button" class="boton" [mat-dialog-close]="false">Cancelar</button>
      <button
        type="button"
        class="boton"
        [class.boton-primario]="!datos.peligro"
        [class.boton-peligro]="datos.peligro"
        [mat-dialog-close]="true"
        cdkFocusInitial
      >
        {{ datos.confirmar }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .detalle {
      margin: 0.75rem 0 0;
      padding-left: 1.125rem;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
  `,
})
export class ConfirmarDialogo {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
  protected readonly ref = inject(MatDialogRef<ConfirmarDialogo, boolean>);
}

/** Abre el dialogo y emite true solo si el usuario confirmo. */
export function confirmar(dialog: MatDialog, datos: DatosConfirmacion): Observable<boolean | undefined> {
  return dialog.open<ConfirmarDialogo, DatosConfirmacion, boolean>(ConfirmarDialogo, { data: datos, width: '480px' })
    .afterClosed();
}
