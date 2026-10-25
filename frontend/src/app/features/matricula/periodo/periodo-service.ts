import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Periodo, PeriodoRequest } from './periodo.model';

@Injectable({ providedIn: 'root' })
export class PeriodoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/periodos';

  listar(): Observable<Periodo[]> {
    return this.http.get<Periodo[]>(this.api.buildUrl(this.resource));
  }

  obtener(id: number): Observable<Periodo> {
    return this.http.get<Periodo>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  crear(periodo: PeriodoRequest): Observable<Periodo> {
    return this.http.post<Periodo>(this.api.buildUrl(this.resource), periodo);
  }

  actualizar(id: number, periodo: PeriodoRequest): Observable<Periodo> {
    return this.http.put<Periodo>(this.api.buildUrl(`${this.resource}/${id}`), periodo);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(this.api.buildUrl(`${this.resource}/${id}`));
  }
}
