import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  buildUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${this.baseUrl}${normalizedPath}`;
  }

  /** true si la peticion va al backend de LAMB (solo a el se le adjunta el token). */
  esDelBackend(url: string): boolean {
    return url.startsWith(this.baseUrl);
  }
}
