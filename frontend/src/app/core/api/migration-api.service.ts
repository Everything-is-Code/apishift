import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { API_BASE_URL } from './api-base';
import type {
  ApplyResultDto,
  BulkRevertResultDto,
  DriftEntryDto,
  ImportExportResponseDto,
  MigrationPlanDto,
  TestCommandDto,
} from './generated';
import type {
  ApplyResult,
  BulkRevertResult,
  ImportExportResponse,
  MigrationPlan,
} from './models';

@Injectable({ providedIn: 'root' })
export class MigrationApiService {
  private readonly baseUrl = `${API_BASE_URL}/migration`;

  constructor(private http: HttpClient) {}

  analyze(gatewayStrategy: string, products: string[], targetClusterId?: string): Observable<MigrationPlan> {
    return this.http.post<MigrationPlanDto>(`${this.baseUrl}/analyze`, {
      gatewayStrategy,
      products,
      targetClusterId: targetClusterId || 'local',
    }) as unknown as Observable<MigrationPlan>;
  }

  importExport(file: File): Observable<ImportExportResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<ImportExportResponseDto>(`${this.baseUrl}/import-export`, formData).pipe(
      timeout(120000)
    ) as unknown as Observable<ImportExportResponse>;
  }

  getPlans(): Observable<MigrationPlan[]> {
    return this.http.get<MigrationPlanDto[]>(`${this.baseUrl}/plans`) as unknown as Observable<MigrationPlan[]>;
  }

  checkDrift(planId: string): Observable<DriftEntryDto[]> {
    return this.http.get<DriftEntryDto[]>(`${this.baseUrl}/plans/${planId}/drift`);
  }

  applyPlan(
    planId: string,
    excludedIndexes?: number[],
    yamlOverrides?: Record<string, string>
  ): Observable<ApplyResult> {
    const body: Record<string, unknown> = {};
    if (excludedIndexes && excludedIndexes.length > 0) body['excludedIndexes'] = excludedIndexes;
    if (yamlOverrides && Object.keys(yamlOverrides).length > 0) body['yamlOverrides'] = yamlOverrides;
    return this.http.post<ApplyResultDto>(`${this.baseUrl}/plans/${planId}/apply`, body) as unknown as Observable<ApplyResult>;
  }

  revertPlan(planId: string): Observable<ApplyResult> {
    return this.http.post<ApplyResultDto>(`${this.baseUrl}/plans/${planId}/revert`, {}) as unknown as Observable<ApplyResult>;
  }

  revertBulk(planIds: string[], deleteGateway: boolean): Observable<BulkRevertResult> {
    return this.http.post<BulkRevertResultDto>(`${this.baseUrl}/revert-bulk`, { planIds, deleteGateway }) as unknown as Observable<BulkRevertResult>;
  }

  getTestCommands(planId: string): Observable<TestCommandDto[]> {
    return this.http.get<TestCommandDto[]>(`${this.baseUrl}/plans/${planId}/test-commands`);
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
