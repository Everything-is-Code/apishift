import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout, retry } from 'rxjs/operators';
import { API_BASE_URL } from './api-base';
import {
  ThreeScaleBackend,
  ThreeScaleProduct,
  ThreeScaleRefreshResult,
  ThreeScaleSource,
  ThreeScaleStatus,
} from './models';

@Injectable({ providedIn: 'root' })
export class ThreeScaleApiService {
  private readonly baseUrl = `${API_BASE_URL}/threescale`;

  constructor(private http: HttpClient) {}

  getProducts(): Observable<ThreeScaleProduct[]> {
    return this.http.get<ThreeScaleProduct[]>(`${this.baseUrl}/products`).pipe(
      timeout(120000),
      retry({ count: 1, delay: 3000 })
    );
  }

  getBackends(): Observable<ThreeScaleBackend[]> {
    return this.http.get<ThreeScaleBackend[]>(`${this.baseUrl}/backends`).pipe(
      timeout(120000),
      retry({ count: 1, delay: 3000 })
    );
  }

  getStatus(): Observable<ThreeScaleStatus> {
    return this.http.get<ThreeScaleStatus>(`${this.baseUrl}/status`);
  }

  refreshDiscovery(): Observable<ThreeScaleRefreshResult> {
    return this.http.post<ThreeScaleRefreshResult>(`${this.baseUrl}/refresh`, {}).pipe(
      timeout(120000)
    );
  }

  getSources(): Observable<ThreeScaleSource[]> {
    return this.http.get<ThreeScaleSource[]>(`${this.baseUrl}/sources`);
  }

  addSource(source: ThreeScaleSource): Observable<ThreeScaleSource> {
    return this.http.post<ThreeScaleSource>(`${this.baseUrl}/sources`, source);
  }

  removeSource(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sources/${id}`);
  }

  getSourceStatus(id: string): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.baseUrl}/sources/${id}/status`);
  }
}
