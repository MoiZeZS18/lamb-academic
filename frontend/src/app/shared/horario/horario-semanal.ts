import { Component, computed, input } from '@angular/core';
import { DiaSemana } from '../../features/matricula/seccion/seccion.model';
import { DIA_CORTO, HORA_MAXIMA, HORA_MINIMA, NOMBRE_DIA, aMinutos } from './horario-util';

export interface BloqueSemanal {
  /** Identifica la seccion (bloques de la misma seccion forman una sola clase). */
  clave: string;
  dia: DiaSemana;
  inicio: string;
  fin: string;
  titulo: string;
  subtitulo?: string;
  detalle?: string;
  conflicto?: boolean;
  /** Clave de color (p. ej. el codigo del curso): el mismo curso conserva su color. Por defecto, `clave`. */
  grupoColor?: string;
  /** Nombre completo para la leyenda y la vista de agenda. */
  nombre?: string;
}

interface Ubicado {
  bloque: BloqueSemanal;
  carril: number;
  carriles: number;
}

const ALTO_HORA = 56;
const DIAS_LABORABLES: DiaSemana[] = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];
/** Date.getDay(): 0 = domingo. */
const DIA_POR_INDICE: DiaSemana[] = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'];

/**
 * Vista semanal (lunes a sabado, franja 07:00-22:00 del backend). Cada bloque se ubica por su hora
 * real; un mismo curso conserva su color; los bloques que se cruzan se dibujan lado a lado. En
 * pantallas angostas se muestra como agenda por dia. Sin bloques no dibuja clases.
 */
@Component({
  selector: 'app-horario-semanal',
  templateUrl: './horario-semanal.html',
  styleUrl: './horario-semanal.css',
})
export class HorarioSemanal {
  readonly bloques = input<BloqueSemanal[]>([]);
  readonly titulo = input('Horario semanal');
  readonly mostrarLeyenda = input(false);

  protected readonly altoHora = ALTO_HORA;
  protected readonly nombreDia = NOMBRE_DIA;
  protected readonly diaCorto = DIA_CORTO;
  protected readonly hoy = DIA_POR_INDICE[new Date().getDay()];

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

  protected readonly columnas = computed(() => `64px repeat(${this.dias().length}, minmax(0, 1fr))`);

  protected readonly altoTotal = computed(() => (this.horaFin() - this.horaInicio()) * ALTO_HORA);

  private readonly gruposColor = computed(() => [...new Set(this.bloques().map((b) => b.grupoColor ?? b.clave))]);

  /** Leyenda: un elemento por grupo de color (normalmente, por curso). */
  protected readonly leyenda = computed(() => {
    const vistos = new Map<string, BloqueSemanal>();
    for (const b of this.bloques()) {
      const grupo = b.grupoColor ?? b.clave;
      if (!vistos.has(grupo)) vistos.set(grupo, b);
    }
    return [...vistos.entries()].map(([grupo, b]) => ({ grupo, titulo: b.titulo, nombre: b.nombre, color: this.color(b) }));
  });

  /** Agenda por dia (pantallas angostas): solo los dias con clases, en orden de hora. */
  protected readonly agenda = computed(() =>
    this.dias()
      .map((dia) => ({
        dia,
        clases: this.bloques()
          .filter((b) => b.dia === dia)
          .sort((a, b) => aMinutos(a.inicio) - aMinutos(b.inicio)),
      }))
      .filter((d) => d.clases.length > 0),
  );

  /**
   * Bloques del dia con su carril: los que se solapan (un cruce) se dibujan lado a lado en vez de
   * taparse, asi el cruce se ve. Cada grupo de bloques solapados reparte el ancho de la columna.
   */
  protected bloquesDe(dia: DiaSemana): Ubicado[] {
    const delDia = this.bloques()
      .filter((b) => b.dia === dia)
      .sort((a, b) => aMinutos(a.inicio) - aMinutos(b.inicio));
    const resultado: Ubicado[] = [];
    let grupo: Ubicado[] = [];
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

  protected color(bloque: BloqueSemanal): number {
    return this.gruposColor().indexOf(bloque.grupoColor ?? bloque.clave) % 8;
  }

  protected duracion(bloque: BloqueSemanal): number {
    return aMinutos(bloque.fin) - aMinutos(bloque.inicio);
  }
}
