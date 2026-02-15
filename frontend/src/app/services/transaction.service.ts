import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction, CreateTransactionRequest, UpdateTransactionRequest, LineItemRequest } from '../models/transaction.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = `${environment.apiUrl}/transactions`;

  constructor(private http: HttpClient) {}

  /**
   * Get all transactions for user with optional filtering
   */
  getTransactions(category?: string, startDate?: string, endDate?: string): Observable<Transaction[]> {
    let params = new HttpParams();

    if (category) {
      params = params.set('category', category);
    }
    if (startDate && endDate) {
      params = params.set('startDate', startDate);
      params = params.set('endDate', endDate);
    }

    return this.http.get<Transaction[]>(this.apiUrl, { params });
  }

  /**
   * Get single transaction by ID
   */
  getTransaction(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.apiUrl}/${id}`);
  }

  /**
   * Upload receipt image and process with AI
   */
  processReceipt(file: File): Observable<Transaction> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Transaction>(`${this.apiUrl}/process`, formData);
  }

  /**
   * Create transaction manually
   */
  createTransaction(transaction: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl, transaction);
  }

  /**
   * Update transaction
   */
  updateTransaction(id: number, updates: CreateTransactionRequest): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.apiUrl}/${id}`, updates);
  }

  /**
   * Delete transaction
   */
  deleteTransaction(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  /**
   * Add line item to transaction
   */
  addLineItem(transactionId: number, item: LineItemRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/${transactionId}/items`, item);
  }

  /**
   * Update line item in transaction
   */
  updateLineItem(transactionId: number, itemIndex: number, item: LineItemRequest): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.apiUrl}/${transactionId}/items/${itemIndex}`, item);
  }

  /**
   * Delete line item from transaction
   */
  deleteLineItem(transactionId: number, itemIndex: number): Observable<Transaction> {
    return this.http.delete<Transaction>(`${this.apiUrl}/${transactionId}/items/${itemIndex}`);
  }

  /**
   * Get transaction statistics
   */
  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }
}
