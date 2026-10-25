import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api-service';

/** CargoResponse de Finanzas: el cargo lo genera el backend al registrar la matricula. */
export type EstadoCargo = 'EMITIDO' | 'PAGADO_PARCIAL' | 'PAGADO_TOTAL' | 'ANULADO';

export interface Cuota {
  id: number;
  numeroCuota: number;
  conceptoNombre: string | null;
  fechaVencimiento: string;
  montoCuota: number;
  montoPagado: number;
  estado: 'PENDIENTE' | 'PAGADA_PARCIAL' | 'PAGADA' | 'ANULADA';
}

export interface Cargo {
  id: number;
  numeroCargo: string;
  estudianteId: number;
  procesoOrigen: 'MATRICULA' | 'TRAMITE' | 'OTRO';
  referenciaId: number | null;
  periodoId: number;
  totalCreditos: number | null;
  numeroCuotas: number;
  montoTotal: number;
  saldoPendiente: number;
  estado: EstadoCargo;
  observacion: string | null;
  fechaEmision: string;
  cuotas: Cuota[];
}

export interface CargoEstadoReporte {
  estado: EstadoCargo;
  cantidad: number;
  montoTotal: number;
  saldoPendiente: number;
}

@Injectable({ providedIn: 'root' })
export class CargoService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly resource = '/api/v1/finanzas/cargos';

  obtener(id: number): Observable<Cargo> {
    return this.http.get<Cargo>(this.api.buildUrl(`${this.resource}/${id}`));
  }

  resumen(periodoId?: number): Observable<CargoEstadoReporte[]> {
    let params = new HttpParams();
    if (periodoId) params = params.set('periodoId', periodoId);
    return this.http.get<CargoEstadoReporte[]>(this.api.buildUrl(`${this.resource}/resumen`), { params });
  }
}
