import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout, retry } from 'rxjs/operators';
import { API_BASE_URL } from './api-base';
import type {
  ThreeScaleAdminStatusDto,
  ThreeScaleBackendDto,
  ThreeScaleProductDto,
  ThreeScaleRefreshResultDto,
  ThreeScaleSourceStatusDto,
  ThreeScaleSourceViewDto,
} from './generated';
import type {
  ThreeScaleProduct,
  ThreeScaleSource,
  ThreeScaleSourceView,
  ThreeScaleStatus,
} from './models';

@Injectable({ providedIn: 'root' })
export class ThreeScaleApiService {
  private readonly baseUrl = `${API_BASE_URL}/threescale`;

  constructor(private http: HttpClient) {}

  getProducts(): Observable<ThreeScaleProduct[]> {
    return this.http.get<ThreeScaleProductDto[]>(`${this.baseUrl}/products`).pipe(
      timeout(120000),
      retry({ count: 1, delay: 3000 })
    ) as unknown as Observable<ThreeScaleProduct[]>;
  }

  getBackends(): Observable<ThreeScaleBackendDto[]> {
    return this.http.get<ThreeScaleBackendDto[]>(`${this.baseUrl}/backends`).pipe(
      timeout(120000),
      retry({ count: 1, delay: 3000 })
    );
  }

  getStatus(): Observable<ThreeScaleStatus> {
    return this.http.get<ThreeScaleAdminStatusDto>(`${this.baseUrl}/status`) as unknown as Observable<ThreeScaleStatus>;
  }

  refreshDiscovery(): Observable<ThreeScaleRefreshResultDto> {
    return this.http.post<ThreeScaleRefreshResultDto>(`${this.baseUrl}/refresh`, {}).pipe(
      timeout(120000)
    );
  }

  getSources(): Observable<ThreeScaleSourceView[]> {
    return this.http.get<ThreeScaleSourceViewDto[]>(`${this.baseUrl}/sources`);
  }

  addSource(source: ThreeScaleSource): Observable<ThreeScaleSourceView> {
    return this.http.post<ThreeScaleSourceViewDto>(`${this.baseUrl}/sources`, source);
  }

  removeSource(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sources/${id}`);
  }

  getSourceStatus(id: string): Observable<ThreeScaleSourceStatusDto> {
    return this.http.get<ThreeScaleSourceStatusDto>(`${this.baseUrl}/sources/${id}/status`);
  }
}
