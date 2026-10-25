import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';

/** PersonaResponse de tipo DOCENTE (GET /api/v1/docentes), solo los campos que usa Matricula. */
export interface Docente {
  id: number;
  nombreCompleto: string;
  email: string | null;
  activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class DocenteService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  listar(activo = true): Observable<Docente[]> {
    const params = new HttpParams().set('activo', activo);
    return this.http.get<Docente[]>(this.api.buildUrl('/api/v1/docentes'), { params });
  }
}
