import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WeeklySpending {
  week: string;
  amount: number;
  startDate: string;
  endDate: string;
}

export interface MonthlySpending {
  month: string;
  amount: number;
  year: number;
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
  percentage: number;
  count: number;
}

export interface SpendingTrend {
  date: string;
  amount: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private apiUrl = `${environment.apiUrl}/analytics`;

  constructor(private http: HttpClient) {}

  getWeeklySpending(weeks: number = 4): Observable<WeeklySpending[]> {
    const params = new HttpParams().set('weeks', weeks.toString());
    return this.http.get<WeeklySpending[]>(`${this.apiUrl}/weekly-spending`, { params });
  }

  getMonthlySpending(months: number = 6): Observable<MonthlySpending[]> {
    const params = new HttpParams().set('months', months.toString());
    return this.http.get<MonthlySpending[]>(`${this.apiUrl}/monthly-spending`, { params });
  }

  getCategoryBreakdown(period: 'WEEKLY' | 'MONTHLY' | 'YEARLY' = 'MONTHLY'): Observable<CategoryBreakdown[]> {
    const params = new HttpParams().set('period', period);
    return this.http.get<CategoryBreakdown[]>(`${this.apiUrl}/category-breakdown`, { params });
  }

  getTopCategories(limit: number = 5): Observable<CategoryBreakdown[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<CategoryBreakdown[]>(`${this.apiUrl}/top-categories`, { params });
  }

  getBottomCategories(limit: number = 5): Observable<CategoryBreakdown[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<CategoryBreakdown[]>(`${this.apiUrl}/bottom-categories`, { params });
  }

  getSpendingTrends(startDate: string, endDate: string): Observable<SpendingTrend[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<SpendingTrend[]>(`${this.apiUrl}/spending-trends`, { params });
  }
}
