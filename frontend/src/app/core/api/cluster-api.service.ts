import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-base';
import type {
  ClusterFeaturesDto,
  ClusterReadinessDto,
  ProjectInfoDto,
  TargetClusterDto,
} from './generated';
import type {
  ClusterReadiness,
  FeatureFlags,
  ProjectInfo,
  TargetCluster,
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

  getTargetClusters(): Observable<TargetCluster[]> {
    return this.http.get<TargetClusterDto[]>(`${this.baseUrl}/targets`) as unknown as Observable<TargetCluster[]>;
  }

  addTargetCluster(cluster: TargetCluster): Observable<TargetCluster> {
    return this.http.post<TargetClusterDto>(`${this.baseUrl}/targets`, cluster) as unknown as Observable<TargetCluster>;
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
