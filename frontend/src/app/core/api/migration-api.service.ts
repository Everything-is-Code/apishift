import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { API_BASE_URL } from './api-base';
import {
  ApplyResult,
  BulkRevertResult,
  DriftEntry,
  ImportExportResponse,
  MigrationPlan,
  TestCommand,
} from './models';

@Injectable({ providedIn: 'root' })
export class MigrationApiService {
  private readonly baseUrl = `${API_BASE_URL}/migration`;

  constructor(private http: HttpClient) {}

  analyze(gatewayStrategy: string, products: string[], targetClusterId?: string): Observable<MigrationPlan> {
    return this.http.post<MigrationPlan>(`${this.baseUrl}/analyze`, {
      gatewayStrategy,
      products,
      targetClusterId: targetClusterId || 'local',
    });
  }

  importExport(file: File): Observable<ImportExportResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<ImportExportResponse>(`${this.baseUrl}/import-export`, formData).pipe(
      timeout(120000)
    );
  }

  getPlans(): Observable<MigrationPlan[]> {
    return this.http.get<MigrationPlan[]>(`${this.baseUrl}/plans`);
  }

  checkDrift(planId: string): Observable<DriftEntry[]> {
    return this.http.get<DriftEntry[]>(`${this.baseUrl}/plans/${planId}/drift`);
  }

  applyPlan(
    planId: string,
    excludedIndexes?: number[],
    yamlOverrides?: Record<string, string>
  ): Observable<ApplyResult> {
    const body: Record<string, unknown> = {};
    if (excludedIndexes && excludedIndexes.length > 0) body['excludedIndexes'] = excludedIndexes;
    if (yamlOverrides && Object.keys(yamlOverrides).length > 0) body['yamlOverrides'] = yamlOverrides;
    return this.http.post<ApplyResult>(`${this.baseUrl}/plans/${planId}/apply`, body);
  }

  revertPlan(planId: string): Observable<ApplyResult> {
    return this.http.post<ApplyResult>(`${this.baseUrl}/plans/${planId}/revert`, {});
  }

  revertBulk(planIds: string[], deleteGateway: boolean): Observable<BulkRevertResult> {
    return this.http.post<BulkRevertResult>(`${this.baseUrl}/revert-bulk`, { planIds, deleteGateway });
  }

  getTestCommands(planId: string): Observable<TestCommand[]> {
    return this.http.get<TestCommand[]>(`${this.baseUrl}/plans/${planId}/test-commands`);
  }

  getCatalogInfo(planId: string, productName: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/plans/${planId}/catalog-info/${productName}`, { responseType: 'text' });
  }

  confirmRegistration(planId: string, componentYaml?: string): Observable<Record<string, string>> {
    return this.http.post<Record<string, string>>(
      `${this.baseUrl}/plans/${planId}/confirm-registration`,
      { componentYaml: componentYaml || '' }
    ).pipe(timeout(60000));
  }
}
