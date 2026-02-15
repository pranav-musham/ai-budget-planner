import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ChatMessage {
  id: number;
  message: string;
  response: string;
  timestamp: string;
}

export interface ChatMessageRequest {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = `${environment.apiUrl}/chat`;

  constructor(private http: HttpClient) { }

  /**
   * Send a message to AI assistant
   */
  sendMessage(message: string): Observable<ChatMessage> {
    const request: ChatMessageRequest = { message };
    return this.http.post<ChatMessage>(`${this.apiUrl}/message`, request);
  }

  /**
   * Get chat history
   */
  getChatHistory(limit: number = 50): Observable<ChatMessage[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/history`, { params });
  }

  /**
   * Clear chat history
   */
  clearChatHistory(): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/history`);
  }

  /**
   * Health check
   */
  healthCheck(): Observable<{ status: string; service: string }> {
    return this.http.get<{ status: string; service: string }>(`${this.apiUrl}/health`);
  }
}
