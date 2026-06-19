import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-base';
import type {
  ClusterFeaturesDto,
  ClusterReadinessDto,
  ProjectInfoDto,
  TargetClusterViewDto,
} from './generated';
import type {
  ClusterReadiness,
  FeatureFlags,
  ProjectInfo,
  TargetCluster,
  TargetClusterView,
} from './models';

@Injectable({ providedIn: 'root' })
export class ClusterApiService {
  private readonly baseUrl = `${API_BASE_URL}/cluster`;

  constructor(private http: HttpClient) {}

  getProjects(): Observable<ProjectInfo[]> {
    return this.http.get<ProjectInfoDto[]>(`${this.baseUrl}/projects`) as unknown as Observable<ProjectInfo[]>;
  }

  getFeatures(): Observable<FeatureFlags> {
    return this.http.get<ClusterFeaturesDto>(`${this.baseUrl}/features`) as unknown as Observable<FeatureFlags>;
  }

  getTargetClusters(): Observable<TargetClusterView[]> {
    return this.http.get<TargetClusterViewDto[]>(`${this.baseUrl}/targets`);
  }

  addTargetCluster(cluster: TargetCluster): Observable<TargetClusterView> {
    return this.http.post<TargetClusterViewDto>(`${this.baseUrl}/targets`, cluster);
  }

  removeTargetCluster(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/targets/${id}`);
  }

  validateTargetCluster(id: string): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.baseUrl}/targets/${id}/validate`);
  }

  getClusterReadiness(targetClusterId?: string, planId?: string): Observable<ClusterReadiness> {
    const params: string[] = [];
    if (targetClusterId) params.push(`targetClusterId=${encodeURIComponent(targetClusterId)}`);
    if (planId) params.push(`planId=${encodeURIComponent(planId)}`);
    const query = params.length ? `?${params.join('&')}` : '';
    return this.http.get<ClusterReadinessDto>(`${this.baseUrl}/readiness${query}`) as unknown as Observable<ClusterReadiness>;
  }
}
