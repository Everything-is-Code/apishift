import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { AuditEntry } from './models';

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly baseUrl = `${API_BASE_URL}/audit`;

  constructor(private http: HttpClient) {}

  getReports(): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(`${this.baseUrl}/reports`);
  }
}
