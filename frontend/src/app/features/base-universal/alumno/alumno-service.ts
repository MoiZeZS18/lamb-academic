import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';

/** AlumnoResponse de Base Universal. Solo lo consulta el personal (SecurityConfig). */
export interface Alumno {
  id: number;
  codigoEstudiante: string;
  estado: 'ACTIVO' | 'SUSPENDIDO' | 'RETIRADO' | 'EGRESADO';
  fechaIngreso: string;
  habilitado: boolean;
  persona: {
    id: number;
    numeroDocumento: string;
    nombreCompleto: string;
    email: string | null;
    idEscuela: number | null;
    escuelaNombre: string | null;
  };
}

@Injectable({ providedIn: 'root' })
export class AlumnoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  listar(estado?: Alumno['estado']): Observable<Alumno[]> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    return this.http.get<Alumno[]>(this.api.buildUrl('/api/v1/alumnos'), { params });
  }
}
