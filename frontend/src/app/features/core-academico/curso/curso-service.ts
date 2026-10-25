import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';
import { Curso, CursoResumen } from './curso.model';

/** Consultas de Core Academico que Matricula necesita (sin pantallas propias en esta unidad). */
@Injectable({ providedIn: 'root' })
export class CursoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/cursos';

  listar(activo?: boolean): Observable<Curso[]> {
    let params = new HttpParams();
    if (activo !== undefined) params = params.set('activo', activo);
    return this.http.get<Curso[]>(this.api.buildUrl(this.resource), { params });
  }

  prerrequisitos(cursoId: number): Observable<CursoResumen[]> {
    return this.http.get<CursoResumen[]>(this.api.buildUrl(`${this.resource}/${cursoId}/prerrequisitos`));
  }
}
