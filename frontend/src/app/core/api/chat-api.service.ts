import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { ChatMessage } from './models';

@Injectable({ providedIn: 'root' })
export class ChatApiService {
  private readonly baseUrl = `${API_BASE_URL}/chat`;

  constructor(private http: HttpClient) {}

  send(message: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(this.baseUrl, {
      role: 'user',
      content: message,
    });
  }
}
