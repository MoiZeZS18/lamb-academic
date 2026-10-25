import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import {
  EstadoSeccion,
  Seccion,
  SeccionActualizacionRequest,
  SeccionFiltros,
  SeccionOcupacion,
  SeccionRequest,
} from './seccion.model';

@Injectable({ providedIn: 'root' })
export class SeccionService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/secciones';

  /** Los filtros se resuelven en el servidor (S8 2.5); solo se envian los que tienen valor. */
  listar(filtros: SeccionFiltros = {}): Observable<Seccion[]> {
    let params = new HttpParams();
    if (filtros.periodoId) params = params.set('periodoId', filtros.periodoId);
    if (filtros.cursoId) params = params.set('cursoId', filtros.cursoId);
    if (filtros.docenteId) params = params.set('docenteId', filtros.docenteId);
    if (filtros.estado) params = params.set('estado', filtros.estado);
    if (filtros.conVacantes) params = params.set('conVacantes', true);
    return this.http.get<Seccion[]>(this.api.buildUrl(this.resource), { params });
  }

  obtener(id: number): Observable<Seccion> {
    return this.http.get<Seccion>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  crear(seccion: SeccionRequest): Observable<Seccion> {
    return this.http.post<Seccion>(this.api.buildUrl(this.resource), seccion);
  }

  actualizar(id: number, cambios: SeccionActualizacionRequest): Observable<Seccion> {
    return this.http.put<Seccion>(this.api.buildUrl(`${this.resource}/${id}`), cambios);
  }

  cambiarEstado(id: number, estado: EstadoSeccion): Observable<Seccion> {
    return this.http.patch<Seccion>(this.api.buildUrl(`${this.resource}/${id}/estado`), { estado });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  ocupacion(periodoId: number): Observable<SeccionOcupacion[]> {
    const params = new HttpParams().set('periodoId', periodoId);
    return this.http.get<SeccionOcupacion[]>(this.api.buildUrl(`${this.resource}/resumen`), { params });
  }
}
