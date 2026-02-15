import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TransactionService } from '../../services/transaction.service';
import { Transaction } from '../../models/transaction.model';

export interface CategorySpend {
  category: string;
  amount: number;
  count: number;
  percentage: number;
}

export interface MonthGroup {
  label: string;
  key: string;
  total: number;
  transactions: Transaction[];
  categoryBreakdown: CategorySpend[];
}

@Component({
  selector: 'app-receipt-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule
  ],
  templateUrl: './receipt-list.html',
  styleUrl: './receipt-list.css'
})
export class ReceiptListComponent implements OnInit {
  transactions: Transaction[] = [];
  monthGroups: MonthGroup[] = [];
  loading = false;
  collapsedMonths = new Set<string>();

  constructor(
    private transactionService: TransactionService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadTransactions();
  }

  loadTransactions() {
    this.loading = true;
    this.transactionService.getTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions.sort((a, b) =>
          new Date(b.transactionDate + 'T00:00:00').getTime() - new Date(a.transactionDate + 'T00:00:00').getTime()
        );
        this.groupByMonth();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading transactions:', error);
        this.loading = false;
      }
    });
  }

  private groupByMonth(): void {
    const groups = new Map<string, MonthGroup>();

    for (const txn of this.transactions) {
      const date = new Date(txn.transactionDate + 'T00:00:00');
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      const label = date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

      if (!groups.has(key)) {
        groups.set(key, { label, key, total: 0, transactions: [], categoryBreakdown: [] });
      }

      const group = groups.get(key)!;
      group.transactions.push(txn);
      group.total += txn.amount || 0;
    }

    // Compute category breakdown for each month
    for (const group of groups.values()) {
      const catMap = new Map<string, { amount: number; count: number }>();

      for (const txn of group.transactions) {
        const cat = txn.category || 'Other';
        if (!catMap.has(cat)) {
          catMap.set(cat, { amount: 0, count: 0 });
        }
        const entry = catMap.get(cat)!;
        entry.amount += txn.amount || 0;
        entry.count++;
      }

      group.categoryBreakdown = Array.from(catMap.entries())
        .map(([category, data]) => ({
          category,
          amount: data.amount,
          count: data.count,
          percentage: group.total > 0 ? (data.amount / group.total) * 100 : 0
        }))
        .sort((a, b) => b.amount - a.amount);
    }

    this.monthGroups = Array.from(groups.values()).sort((a, b) => b.key.localeCompare(a.key));
  }

  toggleMonth(key: string) {
    if (this.collapsedMonths.has(key)) {
      this.collapsedMonths.delete(key);
    } else {
      this.collapsedMonths.add(key);
    }
  }

  isCollapsed(key: string): boolean {
    return this.collapsedMonths.has(key);
  }

  getCategoryIcon(category: string): string {
    const icons: { [key: string]: string } = {
      'Food': 'restaurant',
      'Groceries': 'shopping_cart',
      'Transport': 'directions_car',
      'Shopping': 'shopping_bag',
      'Bills': 'receipt',
      'Entertainment': 'movie',
      'Health': 'health_and_safety',
      'Other': 'category'
    };
    return icons[category] || 'category';
  }

  getCategoryColor(category: string): string {
    const colors: { [key: string]: string } = {
      'Food': '#ef5350',
      'Groceries': '#66bb6a',
      'Transport': '#42a5f5',
      'Shopping': '#ab47bc',
      'Bills': '#ff7043',
      'Entertainment': '#ec407a',
      'Health': '#26a69a',
      'Other': '#78909c'
    };
    return colors[category] || '#78909c';
  }

  viewTransaction(id: number | undefined) {
    if (id) {
      this.router.navigate(['/transactions', id]);
    }
  }

  deleteTransaction(event: Event, id: number | undefined) {
    event.stopPropagation();
    if (id && confirm('Are you sure you want to delete this transaction?')) {
      this.transactionService.deleteTransaction(id).subscribe({
        next: () => this.loadTransactions(),
        error: (error) => console.error('Error deleting transaction:', error)
      });
    }
  }

  addExpense() {
    this.router.navigate(['/transactions/create']);
  }
}
