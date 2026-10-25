import { Component, computed, input } from '@angular/core';
import { DiaSemana } from '../../features/matricula/seccion/seccion.model';
import { DIA_CORTO, HORA_MAXIMA, HORA_MINIMA, NOMBRE_DIA, aMinutos } from './horario-util';

export interface BloqueSemanal {
  /** Agrupa bloques del mismo curso/seccion (mismo color). */
  clave: string;
  dia: DiaSemana;
  inicio: string;
  fin: string;
  titulo: string;
  subtitulo?: string;
  detalle?: string;
  conflicto?: boolean;
}

const ALTO_HORA = 48;
const DIAS_LABORABLES: DiaSemana[] = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];

/**
 * Vista semanal (lunes a sabado, 07:00-22:00, franja del backend). Cada bloque se ubica por su
 * hora real; los bloques de una misma seccion comparten color. Sin bloques no dibuja clases.
 */
@Component({
  selector: 'app-horario-semanal',
  template: `
    <div class="semana" role="table" [attr.aria-label]="titulo()">
      <div class="fila-encabezado" role="row" [style.grid-template-columns]="columnas()">
        <div class="esquina" role="columnheader"><span class="visualmente-oculto">Hora</span></div>
        @for (dia of dias(); track dia) {
          <div class="dia" role="columnheader">
            <span class="dia-largo">{{ nombreDia[dia] }}</span>
            <span class="dia-corto">{{ diaCorto[dia] }}</span>
          </div>
        }
      </div>
      <div class="cuerpo-semana" [style.height.px]="altoTotal()" [style.grid-template-columns]="columnas()">
        <div class="horas" aria-hidden="true">
          @for (hora of horas(); track hora) {
            <span [style.top.px]="(hora - horaInicio()) * altoHora">{{ hora }}:00</span>
          }
        </div>
        @for (dia of dias(); track dia) {
          <div class="columna" role="row" [attr.aria-label]="nombreDia[dia]">
            @for (hora of horas(); track hora) {
              <span class="linea" [style.top.px]="(hora - horaInicio()) * altoHora"></span>
            }
            @for (ubicado of bloquesDe(dia); track ubicado.bloque.clave + ubicado.bloque.inicio) {
              @let bloque = ubicado.bloque;
              <div
                class="bloque"
                [style.left]="'calc(' + (100 / ubicado.carriles) * ubicado.carril + '% + 3px)'"
                [style.width]="'calc(' + 100 / ubicado.carriles + '% - 6px)'"
                role="cell"
                [class]="'bloque color-' + color(bloque.clave)"
                [class.conflicto]="bloque.conflicto"
                [style.top.px]="posicion(bloque.inicio)"
                [style.height.px]="posicion(bloque.fin) - posicion(bloque.inicio)"
                [attr.title]="bloque.titulo + ' ' + bloque.inicio + '-' + bloque.fin"
              >
                <strong>{{ bloque.titulo }}</strong>
                <span class="hora">{{ bloque.inicio }}–{{ bloque.fin }}</span>
                @if (bloque.subtitulo) {
                  <span>{{ bloque.subtitulo }}</span>
                }
                @if (bloque.detalle) {
                  <span class="detalle">{{ bloque.detalle }}</span>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styleUrl: './horario-semanal.css',
})
export class HorarioSemanal {
  readonly bloques = input<BloqueSemanal[]>([]);
  readonly titulo = input('Horario semanal');

  protected readonly altoHora = ALTO_HORA;
  protected readonly nombreDia = NOMBRE_DIA;
  protected readonly diaCorto = DIA_CORTO;

  /** Domingo solo aparece si alguna seccion lo usa. */
  protected readonly dias = computed<DiaSemana[]>(() =>
    this.bloques().some((b) => b.dia === 'DOMINGO') ? [...DIAS_LABORABLES, 'DOMINGO'] : DIAS_LABORABLES,
  );

  /** Se recorta a las horas con clases (con margen) para no dibujar una grilla vacia enorme. */
  protected readonly horaInicio = computed(() => {
    const minimo = Math.min(...this.bloques().map((b) => Math.floor(aMinutos(b.inicio) / 60)));
    return Number.isFinite(minimo) ? Math.max(HORA_MINIMA, minimo - 1) : 8;
  });

  protected readonly horaFin = computed(() => {
    const maximo = Math.max(...this.bloques().map((b) => Math.ceil(aMinutos(b.fin) / 60)));
    return Number.isFinite(maximo) ? Math.min(HORA_MAXIMA, maximo + 1) : 14;
  });

  protected readonly horas = computed(() =>
    Array.from({ length: this.horaFin() - this.horaInicio() + 1 }, (_, i) => this.horaInicio() + i),
  );

  protected readonly columnas = computed(() => `56px repeat(${this.dias().length}, minmax(0, 1fr))`);

  protected readonly altoTotal = computed(() => (this.horaFin() - this.horaInicio()) * ALTO_HORA);

  private readonly claves = computed(() => [...new Set(this.bloques().map((b) => b.clave))]);

  /**
   * Bloques del dia con su carril: los que se solapan (un cruce) se dibujan lado a lado en vez de
   * taparse, asi el cruce se ve. Cada grupo de bloques solapados reparte el ancho de la columna.
   */
  protected bloquesDe(dia: DiaSemana): { bloque: BloqueSemanal; carril: number; carriles: number }[] {
    const delDia = this.bloques()
      .filter((b) => b.dia === dia)
      .sort((a, b) => aMinutos(a.inicio) - aMinutos(b.inicio));
    const resultado: { bloque: BloqueSemanal; carril: number; carriles: number }[] = [];
    let grupo: { bloque: BloqueSemanal; carril: number; carriles: number }[] = [];
    let finGrupo = -1;
    const cerrarGrupo = () => {
      const carriles = Math.max(1, ...grupo.map((g) => g.carril + 1));
      grupo.forEach((g) => (g.carriles = carriles));
      resultado.push(...grupo);
      grupo = [];
    };
    for (const bloque of delDia) {
      if (aMinutos(bloque.inicio) >= finGrupo) cerrarGrupo();
      const ocupados = new Set(
        grupo.filter((g) => aMinutos(g.bloque.fin) > aMinutos(bloque.inicio)).map((g) => g.carril),
      );
      let carril = 0;
      while (ocupados.has(carril)) carril++;
      grupo.push({ bloque, carril, carriles: 1 });
      finGrupo = Math.max(finGrupo, aMinutos(bloque.fin));
    }
    cerrarGrupo();
    return resultado;
  }

  protected posicion(hora: string): number {
    return ((aMinutos(hora) - this.horaInicio() * 60) / 60) * ALTO_HORA;
  }

  protected color(clave: string): number {
    return this.claves().indexOf(clave) % 6;
  }
}
