import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IncomeSource, CreateIncomeRequest } from '../models/income.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class IncomeService {
  private apiUrl = `${environment.apiUrl}/income`;

  constructor(private http: HttpClient) {}

  getIncomeSources(): Observable<IncomeSource[]> {
    return this.http.get<IncomeSource[]>(this.apiUrl);
  }

  getTotalMonthlyIncome(): Observable<{ totalMonthlyIncome: number }> {
    return this.http.get<{ totalMonthlyIncome: number }>(`${this.apiUrl}/total-monthly`);
  }

  getIncomeSource(id: number): Observable<IncomeSource> {
    return this.http.get<IncomeSource>(`${this.apiUrl}/${id}`);
  }

  createIncomeSource(income: CreateIncomeRequest): Observable<IncomeSource> {
    return this.http.post<IncomeSource>(this.apiUrl, income);
  }

  updateIncomeSource(id: number, income: CreateIncomeRequest): Observable<IncomeSource> {
    return this.http.put<IncomeSource>(`${this.apiUrl}/${id}`, income);
  }

  deleteIncomeSource(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
