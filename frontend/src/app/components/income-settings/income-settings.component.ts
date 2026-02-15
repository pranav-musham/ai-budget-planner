import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { IncomeService } from '../../services/income.service';
import { IncomeSource, CreateIncomeRequest } from '../../models/income.model';

export interface SourceBreakdown {
  source: string;
  amount: number;
  count: number;
  percentage: number;
}

export interface IncomeMonthGroup {
  label: string;
  key: string;
  total: number;
  entries: IncomeSource[];
  sourceBreakdown: SourceBreakdown[];
}

@Component({
  selector: 'app-income-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './income-settings.component.html',
  styleUrl: './income-settings.component.css'
})
export class IncomeSettingsComponent implements OnInit {
  incomeForm: FormGroup;
  incomeSources: IncomeSource[] = [];
  monthGroups: IncomeMonthGroup[] = [];
  collapsedMonths = new Set<string>();
  totalMonthlyIncome: number = 0;
  entriesThisMonth: number = 0;
  isLoading = false;
  editingIncome: IncomeSource | null = null;

  paymentMethods: string[] = ['Bank Transfer', 'Cash', 'Check', 'Direct Deposit', 'PayPal', 'Venmo', 'Other'];

  constructor(
    private fb: FormBuilder,
    private incomeService: IncomeService,
    private snackBar: MatSnackBar
  ) {
    this.incomeForm = this.fb.group({
      sourceName: ['', [Validators.required, Validators.maxLength(100)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      transactionDate: [new Date(), Validators.required],
      paymentMethod: ['Direct Deposit'],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.loadIncomeSources();
    this.loadTotalMonthlyIncome();
  }

  loadIncomeSources(): void {
    this.isLoading = true;
    this.incomeService.getIncomeSources().subscribe({
      next: (sources) => {
        this.incomeSources = sources.sort((a, b) =>
          new Date(b.transactionDate + 'T00:00:00').getTime() - new Date(a.transactionDate + 'T00:00:00').getTime()
        );
        this.groupByMonth();
        this.isLoading = false;
        const now = new Date();
        const currentMonth = now.getMonth();
        const currentYear = now.getFullYear();
        this.entriesThisMonth = sources.filter(s => {
          const d = new Date(s.transactionDate + 'T00:00:00');
          return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
        }).length;
      },
      error: (error) => {
        console.error('Error loading income entries:', error);
        this.snackBar.open('Failed to load income entries', 'Close', { duration: 3000 });
        this.isLoading = false;
      }
    });
  }

  loadTotalMonthlyIncome(): void {
    this.incomeService.getTotalMonthlyIncome().subscribe({
      next: (response) => {
        this.totalMonthlyIncome = response.totalMonthlyIncome;
      },
      error: (error) => {
        console.error('Error loading total monthly income:', error);
      }
    });
  }

  onSubmit(): void {
    if (this.incomeForm.invalid) {
      return;
    }

    const formValue = this.incomeForm.value;
    const request: CreateIncomeRequest = {
      sourceName: formValue.sourceName,
      amount: formValue.amount,
      transactionDate: this.formatDate(formValue.transactionDate),
      paymentMethod: formValue.paymentMethod,
      notes: formValue.notes || undefined
    };

    if (this.editingIncome) {
      this.incomeService.updateIncomeSource(this.editingIncome.id!, request).subscribe({
        next: () => {
          this.snackBar.open('Income entry updated successfully', 'Close', { duration: 3000 });
          this.loadIncomeSources();
          this.loadTotalMonthlyIncome();
          this.resetForm();
        },
        error: (error) => {
          console.error('Error updating income entry:', error);
          this.snackBar.open('Failed to update income entry', 'Close', { duration: 3000 });
        }
      });
    } else {
      this.incomeService.createIncomeSource(request).subscribe({
        next: () => {
          this.snackBar.open('Income entry added successfully', 'Close', { duration: 3000 });
          this.loadIncomeSources();
          this.loadTotalMonthlyIncome();
          this.resetForm();
        },
        error: (error) => {
          console.error('Error creating income entry:', error);
          this.snackBar.open('Failed to add income entry', 'Close', { duration: 3000 });
        }
      });
    }
  }

  editIncome(income: IncomeSource): void {
    this.editingIncome = income;
    this.incomeForm.patchValue({
      sourceName: income.sourceName,
      amount: income.amount,
      transactionDate: new Date(income.transactionDate + 'T00:00:00'),
      paymentMethod: income.paymentMethod || 'Other',
      notes: income.notes || ''
    });
  }

  deleteIncome(income: IncomeSource): void {
    if (confirm(`Are you sure you want to delete "${income.sourceName}"?`)) {
      this.incomeService.deleteIncomeSource(income.id!).subscribe({
        next: () => {
          this.snackBar.open('Income entry deleted successfully', 'Close', { duration: 3000 });
          this.loadIncomeSources();
          this.loadTotalMonthlyIncome();
        },
        error: (error) => {
          console.error('Error deleting income entry:', error);
          this.snackBar.open('Failed to delete income entry', 'Close', { duration: 3000 });
        }
      });
    }
  }

  resetForm(): void {
    this.editingIncome = null;
    this.incomeForm.reset({
      transactionDate: new Date(),
      paymentMethod: 'Direct Deposit'
    });
  }

  private groupByMonth(): void {
    const groups = new Map<string, IncomeMonthGroup>();

    for (const entry of this.incomeSources) {
      const date = new Date(entry.transactionDate + 'T00:00:00');
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      const label = date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

      if (!groups.has(key)) {
        groups.set(key, { label, key, total: 0, entries: [], sourceBreakdown: [] });
      }

      const group = groups.get(key)!;
      group.entries.push(entry);
      group.total += entry.amount || 0;
    }

    // Compute source breakdown per month
    for (const group of groups.values()) {
      const srcMap = new Map<string, { amount: number; count: number }>();

      for (const entry of group.entries) {
        const src = entry.sourceName || 'Other';
        if (!srcMap.has(src)) {
          srcMap.set(src, { amount: 0, count: 0 });
        }
        const data = srcMap.get(src)!;
        data.amount += entry.amount || 0;
        data.count++;
      }

      group.sourceBreakdown = Array.from(srcMap.entries())
        .map(([source, data]) => ({
          source,
          amount: data.amount,
          count: data.count,
          percentage: group.total > 0 ? (data.amount / group.total) * 100 : 0
        }))
        .sort((a, b) => b.amount - a.amount);
    }

    this.monthGroups = Array.from(groups.values()).sort((a, b) => b.key.localeCompare(a.key));
  }

  toggleMonth(key: string): void {
    if (this.collapsedMonths.has(key)) {
      this.collapsedMonths.delete(key);
    } else {
      this.collapsedMonths.add(key);
    }
  }

  isCollapsed(key: string): boolean {
    return this.collapsedMonths.has(key);
  }

  getSourceIcon(source: string): string {
    const lower = source.toLowerCase();
    if (lower.includes('salary') || lower.includes('wage')) return 'work';
    if (lower.includes('freelance') || lower.includes('contract')) return 'computer';
    if (lower.includes('invest') || lower.includes('dividend')) return 'trending_up';
    if (lower.includes('rent')) return 'home';
    if (lower.includes('gift')) return 'card_giftcard';
    if (lower.includes('refund')) return 'replay';
    if (lower.includes('bonus')) return 'star';
    if (lower.includes('side') || lower.includes('gig')) return 'handyman';
    return 'payments';
  }

  getSourceColor(source: string): string {
    const lower = source.toLowerCase();
    if (lower.includes('salary') || lower.includes('wage')) return '#10b981';
    if (lower.includes('freelance') || lower.includes('contract')) return '#3b82f6';
    if (lower.includes('invest') || lower.includes('dividend')) return '#8b5cf6';
    if (lower.includes('rent')) return '#f59e0b';
    if (lower.includes('gift')) return '#ec4899';
    if (lower.includes('refund')) return '#ef4444';
    if (lower.includes('bonus')) return '#f97316';
    if (lower.includes('side') || lower.includes('gig')) return '#14b8a6';
    return '#6b7280';
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
