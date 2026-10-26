import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import {
  Matricula,
  MatriculaFiltros,
  MatriculaRegistroRequest,
  MatriculaReporte,
  Pagina,
  ResultadoCurso,
} from './matricula.model';

@Injectable({ providedIn: 'root' })
export class MatriculaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/matriculas';

  /** Un ESTUDIANTE recibe solo las suyas: el backend filtra por el alumno_id del token. */
  buscar(
    filtros: MatriculaFiltros,
    page = 0,
    size = 10,
    sortBy = 'fecha',
    direction: 'asc' | 'desc' = 'desc',
  ): Observable<Pagina<Matricula>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('direction', direction);
    if (filtros.estudianteId) params = params.set('estudianteId', filtros.estudianteId);
    if (filtros.periodoId) params = params.set('periodoId', filtros.periodoId);
    if (filtros.estado) params = params.set('estado', filtros.estado);
    return this.http.get<Pagina<Matricula>>(this.api.buildUrl(this.resource), { params });
  }

  /**
   * Todas las matriculas que cumplen los filtros, recorriendo las paginas (el backend limita
   * size a 100). Para reportes que necesitan revisar cada registro, no solo el agregado.
   */
  buscarTodas(filtros: MatriculaFiltros): Observable<Matricula[]> {
    const TAMANO = 100;
    return this.buscar(filtros, 0, TAMANO, 'id', 'asc').pipe(
      switchMap((primera) => {
        const resto = Array.from({ length: Math.max(primera.page.totalPages - 1, 0) }, (_, i) =>
          this.buscar(filtros, i + 1, TAMANO, 'id', 'asc'),
        );
        return resto.length
          ? forkJoin(resto).pipe(map((paginas) => [primera, ...paginas].flatMap((p) => p.content)))
          : of(primera.content);
      }),
    );
  }

  obtener(id: number): Observable<Matricula> {
    return this.http.get<Matricula>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  /** Cabecera + detalles en una sola peticion; el backend genera el cargo en la misma transaccion. */
  registrar(solicitud: MatriculaRegistroRequest): Observable<Matricula> {
    return this.http.post<Matricula>(this.api.buildUrl(this.resource), solicitud);
  }

  anular(id: number): Observable<Matricula> {
    return this.http.patch<Matricula>(this.api.buildUrl(`${this.resource}/${id}/anular`), null);
  }

  /** Solo para matriculas sin cargo (idempotente en el backend: no duplica). */
  generarCargo(id: number, numeroCuotas?: number): Observable<Matricula> {
    let params = new HttpParams();
    if (numeroCuotas) params = params.set('numeroCuotas', numeroCuotas);
    return this.http.post<Matricula>(this.api.buildUrl(`${this.resource}/${id}/cargo`), null, { params });
  }

  registrarResultado(id: number, detalleId: number, resultado: ResultadoCurso): Observable<Matricula> {
    return this.http.patch<Matricula>(
      this.api.buildUrl(`${this.resource}/${id}/detalles/${detalleId}/resultado`),
      { resultado },
    );
  }

  reporte(filtros: MatriculaFiltros): Observable<MatriculaReporte> {
    let params = new HttpParams();
    if (filtros.estudianteId) params = params.set('estudianteId', filtros.estudianteId);
    if (filtros.periodoId) params = params.set('periodoId', filtros.periodoId);
    if (filtros.estado) params = params.set('estado', filtros.estado);
    return this.http.get<MatriculaReporte>(this.api.buildUrl(`${this.resource}/reporte`), { params });
  }
}
