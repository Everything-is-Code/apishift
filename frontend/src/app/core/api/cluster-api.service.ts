import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-base';
import {
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
    return this.http.get<ProjectInfo[]>(`${this.baseUrl}/projects`);
  }

  getFeatures(): Observable<FeatureFlags> {
    return this.http.get<FeatureFlags>(`${this.baseUrl}/features`);
  }

  getTargetClusters(): Observable<TargetCluster[]> {
    return this.http.get<TargetCluster[]>(`${this.baseUrl}/targets`);
  }

  addTargetCluster(cluster: TargetCluster): Observable<TargetCluster> {
    return this.http.post<TargetCluster>(`${this.baseUrl}/targets`, cluster);
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
    return this.http.get<ClusterReadiness>(`${this.baseUrl}/readiness${query}`);
  }
}
