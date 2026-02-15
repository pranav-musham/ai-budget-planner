import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TransactionService } from '../../services/transaction.service';
import { CategoryService } from '../../services/category.service';
import { Transaction, CreateTransactionRequest } from '../../models/transaction.model';
import { Category } from '../../models/category.model';

@Component({
  selector: 'app-receipt-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './receipt-detail.html',
  styleUrl: './receipt-detail.css',
})
export class ReceiptDetail implements OnInit {
  transaction?: Transaction;
  loading = true;
  displayedColumns: string[] = ['name', 'quantity', 'unitPrice', 'price'];

  // Edit mode
  isEditing = false;
  saving = false;
  categories: Category[] = [];
  paymentMethods = ['Cash', 'Credit Card', 'Debit Card', 'Bank Transfer', 'Digital Wallet', 'Other'];
  editForm = {
    merchantName: '',
    amount: 0,
    transactionDate: '',
    category: '',
    paymentMethod: '',
    notes: '',
    items: [] as { name: string; quantity: number; unitPrice: number }[]
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private categoryService: CategoryService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadTransaction(id);
    }
    this.loadCategories();
  }

  loadCategories() {
    this.categoryService.getAllCategories().subscribe({
      next: (categories) => this.categories = categories,
      error: () => {
        this.categories = [
          { name: 'Groceries', type: 'PREDEFINED' },
          { name: 'Dining', type: 'PREDEFINED' },
          { name: 'Transportation', type: 'PREDEFINED' },
          { name: 'Health', type: 'PREDEFINED' },
          { name: 'Shopping', type: 'PREDEFINED' },
          { name: 'Entertainment', type: 'PREDEFINED' },
          { name: 'Bills', type: 'PREDEFINED' },
          { name: 'Travel', type: 'PREDEFINED' },
          { name: 'Education', type: 'PREDEFINED' },
          { name: 'Other', type: 'PREDEFINED' }
        ];
      }
    });
  }

  loadTransaction(id: number) {
    this.loading = true;
    this.transactionService.getTransaction(id).subscribe({
      next: (transaction) => {
        this.transaction = transaction;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading transaction:', error);
        this.loading = false;
        alert('Transaction not found');
        this.router.navigate(['/transactions']);
      }
    });
  }

  goBack() {
    this.router.navigate(['/transactions']);
  }

  // Edit methods
  startEdit() {
    if (!this.transaction) return;
    this.editForm = {
      merchantName: this.transaction.merchantName,
      amount: this.transaction.amount,
      transactionDate: this.transaction.transactionDate,
      category: this.transaction.category,
      paymentMethod: this.transaction.paymentMethod || '',
      notes: this.transaction.notes || '',
      items: (this.transaction.items || []).map(item => ({
        name: item.name,
        quantity: item.quantity || 1,
        unitPrice: item.unitPrice || item.price
      }))
    };
    this.isEditing = true;
  }

  cancelEdit() {
    this.isEditing = false;
  }

  saveEdit() {
    if (!this.transaction?.id) return;
    this.saving = true;

    const request: CreateTransactionRequest = {
      merchantName: this.editForm.merchantName,
      amount: this.editForm.amount,
      transactionDate: this.editForm.transactionDate,
      category: this.editForm.category,
      paymentMethod: this.editForm.paymentMethod || undefined,
      notes: this.editForm.notes || undefined,
      items: this.editForm.items.map(item => ({
        name: item.name,
        quantity: item.quantity,
        unitPrice: item.unitPrice
      }))
    };

    this.transactionService.updateTransaction(this.transaction.id, request).subscribe({
      next: (updated) => {
        this.transaction = updated;
        this.isEditing = false;
        this.saving = false;
      },
      error: (err) => {
        console.error('Failed to save:', err);
        alert('Failed to save changes. Please try again.');
        this.saving = false;
      }
    });
  }

  addItem() {
    this.editForm.items.push({ name: '', quantity: 1, unitPrice: 0 });
  }

  removeItem(index: number) {
    this.editForm.items.splice(index, 1);
  }

  deleteTransaction() {
    if (this.transaction?.id && confirm('Are you sure you want to delete this transaction?')) {
      this.transactionService.deleteTransaction(this.transaction.id).subscribe({
        next: () => {
          this.router.navigate(['/transactions']);
        },
        error: (error) => {
          console.error('Error deleting transaction:', error);
          alert('Failed to delete transaction');
        }
      });
    }
  }
}
