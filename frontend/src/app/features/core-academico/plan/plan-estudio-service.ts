import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { PlanEstudio } from './plan-estudio.model';

/**
 * Consulta de mallas de Core Academico que Matricula necesita para ubicar cada curso en el ciclo
 * del plan vigente de la escuela del alumno. Las pantallas de mallas son de Core Academico.
 */
@Injectable({ providedIn: 'root' })
export class PlanEstudioService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/planes-estudio';

  /** Planes de una escuela en un estado; el backend los ordena por anio descendente. */
  buscar(idEscuela: number, estado: PlanEstudio['estado'] = 'VIGENTE'): Observable<PlanEstudio[]> {
    const params = new HttpParams().set('idEscuela', idEscuela).set('estado', estado);
    return this.http.get<PlanEstudio[]>(this.api.buildUrl(this.resource), { params });
  }
}
