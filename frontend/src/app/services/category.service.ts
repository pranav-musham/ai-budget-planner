import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category } from '../models/category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiUrl = 'http://localhost:8080/api/categories';

  constructor(private http: HttpClient) {}

  /**
   * Get all categories (predefined + custom)
   */
  getAllCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.apiUrl}/all`);
  }

  /**
   * Get predefined categories only
   */
  getPredefinedCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.apiUrl}/predefined`);
  }

  /**
   * Get category names for dropdowns
   */
  getCategoryNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/names`);
  }
}
