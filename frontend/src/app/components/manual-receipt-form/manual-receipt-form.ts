import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { TransactionService } from '../../services/transaction.service';
import { CategoryService } from '../../services/category.service';
import { CreateTransactionRequest, LineItemRequest, Transaction } from '../../models/transaction.model';
import { Category } from '../../models/category.model';

interface EditForm {
  merchantName: string;
  amount: number;
  transactionDate: string;
  category: string;
  items: { name: string; quantity: number; unitPrice: number; price: number }[];
}

@Component({
  selector: 'app-add-expense',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatTabsModule,
    MatProgressBarModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './manual-receipt-form.html',
  styleUrl: './manual-receipt-form.css',
})
export class ManualReceiptForm implements OnInit {
  // Manual form state
  transactionForm: FormGroup;
  categories: Category[] = [];
  paymentMethods = ['Cash', 'Credit Card', 'Debit Card', 'Bank Transfer', 'Digital Wallet', 'Other'];
  submitting = false;

  // Upload state
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  selectedFiles: File[] = [];
  imagePreviews: { file: File; preview: string }[] = [];
  isDragging = false;
  isUploading = false;
  isSaving = false;
  uploadError: string | null = null;
  processedTransactions: Transaction[] = [];
  uploadProgress: { [key: string]: number } = {};

  // Upload edit/review state
  editingIndex: number | null = null;
  editForm: EditForm = { merchantName: '', amount: 0, transactionDate: '', category: '', items: [] };
  uploadCategories = [
    'Groceries', 'Dining', 'Transportation', 'Health', 'Shopping',
    'Entertainment', 'Bills', 'Travel', 'Education', 'Other'
  ];

  constructor(
    private fb: FormBuilder,
    private transactionService: TransactionService,
    private categoryService: CategoryService,
    private router: Router
  ) {
    this.transactionForm = this.fb.group({
      merchantName: ['', [Validators.required, Validators.minLength(2)]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      transactionDate: [new Date(), Validators.required],
      category: ['', Validators.required],
      paymentMethod: [''],
      notes: [''],
      items: this.fb.array([])
    });
  }

  ngOnInit() {
    this.loadCategories();
  }

  // ===== Manual Form Methods =====

  loadCategories() {
    this.categoryService.getAllCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (error) => {
        console.error('Error loading categories:', error);
        this.categories = [
          { name: 'Food', type: 'PREDEFINED' },
          { name: 'Transport', type: 'PREDEFINED' },
          { name: 'Shopping', type: 'PREDEFINED' },
          { name: 'Bills', type: 'PREDEFINED' },
          { name: 'Entertainment', type: 'PREDEFINED' },
          { name: 'Health', type: 'PREDEFINED' },
          { name: 'Groceries', type: 'PREDEFINED' },
          { name: 'Other', type: 'PREDEFINED' }
        ];
      }
    });
  }

  get items(): FormArray {
    return this.transactionForm.get('items') as FormArray;
  }

  addItem() {
    const itemGroup = this.fb.group({
      name: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: ['', [Validators.required, Validators.min(0.01)]]
    });
    this.items.push(itemGroup);
  }

  removeItem(index: number) {
    this.items.removeAt(index);
  }

  calculateItemTotal(index: number): number {
    const item = this.items.at(index).value;
    return item.quantity * item.unitPrice || 0;
  }

  calculateTotal(): number {
    return this.items.controls.reduce((sum, item) => {
      const quantity = item.get('quantity')?.value || 0;
      const unitPrice = item.get('unitPrice')?.value || 0;
      return sum + (quantity * unitPrice);
    }, 0);
  }

  onSubmit() {
    if (this.transactionForm.valid) {
      this.submitting = true;

      const formValue = this.transactionForm.value;
      const request: CreateTransactionRequest = {
        merchantName: formValue.merchantName,
        amount: formValue.amount,
        transactionDate: this.formatDate(formValue.transactionDate),
        category: formValue.category,
        paymentMethod: formValue.paymentMethod || undefined,
        notes: formValue.notes || undefined,
        items: formValue.items.map((item: any) => ({
          name: item.name,
          quantity: item.quantity,
          unitPrice: item.unitPrice
        } as LineItemRequest))
      };

      this.transactionService.createTransaction(request).subscribe({
        next: (transaction) => {
          this.router.navigate(['/transactions', transaction.id]);
        },
        error: (error) => {
          console.error('Error creating transaction:', error);
          this.submitting = false;
          alert('Failed to create transaction. Please try again.');
        }
      });
    }
  }

  cancel() {
    this.router.navigate(['/transactions']);
  }

  private formatDate(date: Date): string {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // ===== Upload Methods =====

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFiles(Array.from(files));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFiles(Array.from(input.files));
    }
  }

  handleFiles(files: File[]): void {
    this.uploadError = null;

    for (const file of files) {
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
      if (!validTypes.includes(file.type)) {
        this.uploadError = `${file.name}: Invalid file type. Please select JPEG, PNG, or GIF`;
        continue;
      }

      const maxSize = 10 * 1024 * 1024;
      if (file.size > maxSize) {
        this.uploadError = `${file.name}: File size must be less than 10MB`;
        continue;
      }

      if (!this.selectedFiles.find(f => f.name === file.name)) {
        this.selectedFiles.push(file);

        const reader = new FileReader();
        reader.onload = (e: ProgressEvent<FileReader>) => {
          this.imagePreviews.push({
            file: file,
            preview: e.target?.result as string
          });
        };
        reader.readAsDataURL(file);
      }
    }
  }

  removeFile(file: File): void {
    this.selectedFiles = this.selectedFiles.filter(f => f !== file);
    this.imagePreviews = this.imagePreviews.filter(p => p.file !== file);
  }

  uploadReceipt(): void {
    if (this.selectedFiles.length === 0) return;

    this.isUploading = true;
    this.uploadError = null;
    this.processedTransactions = [];
    this.uploadProgress = {};

    let completed = 0;
    const total = this.selectedFiles.length;

    this.selectedFiles.forEach((file) => {
      this.uploadProgress[file.name] = 0;

      this.transactionService.processReceipt(file).subscribe({
        next: (transaction) => {
          this.uploadProgress[file.name] = 100;
          this.processedTransactions.push(transaction);
          completed++;

          if (completed === total) {
            this.isUploading = false;
          }
        },
        error: (error) => {
          this.uploadProgress[file.name] = -1;
          completed++;
          console.error(`Upload error for ${file.name}:`, error);

          if (completed === total) {
            this.isUploading = false;
            this.uploadError = 'Some receipts failed to process. Please check individual results.';
          }
        }
      });
    });
  }

  clearUpload(): void {
    this.selectedFiles = [];
    this.imagePreviews = [];
    this.processedTransactions = [];
    this.uploadProgress = {};
    this.uploadError = null;
  }

  triggerFileInput(): void {
    this.fileInput?.nativeElement?.click();
  }

  getProgressColor(progress: number): string {
    if (progress === -1) return 'warn';
    if (progress === 100) return 'accent';
    return 'primary';
  }

  getProgressIcon(progress: number): string {
    if (progress === -1) return 'error';
    if (progress === 100) return 'check_circle';
    return 'hourglass_empty';
  }

  getTotalAmount(): number {
    return this.processedTransactions.reduce((sum, t) => sum + t.amount, 0);
  }

  // Edit/Review methods
  startEdit(index: number): void {
    const t = this.processedTransactions[index];
    this.editingIndex = index;
    this.editForm = {
      merchantName: t.merchantName,
      amount: t.amount,
      transactionDate: t.transactionDate,
      category: t.category,
      items: (t.items || []).map(item => ({
        name: item.name,
        quantity: item.quantity || 1,
        unitPrice: item.unitPrice || item.price,
        price: item.price
      }))
    };
  }

  cancelEdit(): void {
    this.editingIndex = null;
  }

  saveEdit(): void {
    if (this.editingIndex === null) return;
    const t = this.processedTransactions[this.editingIndex];
    if (!t.id) return;

    this.isSaving = true;
    const request = {
      merchantName: this.editForm.merchantName,
      amount: this.editForm.amount,
      transactionDate: this.editForm.transactionDate,
      category: this.editForm.category,
      items: this.editForm.items.map(item => ({
        name: item.name,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        price: item.price
      }))
    };

    this.transactionService.updateTransaction(t.id, request).subscribe({
      next: (updated) => {
        this.processedTransactions[this.editingIndex!] = updated;
        this.editingIndex = null;
        this.isSaving = false;
      },
      error: (err) => {
        console.error('Failed to save edit:', err);
        this.uploadError = 'Failed to save changes. Please try again.';
        this.isSaving = false;
      }
    });
  }

  addEditItem(): void {
    this.editForm.items.push({ name: '', quantity: 1, unitPrice: 0, price: 0 });
  }

  removeEditItem(index: number): void {
    this.editForm.items.splice(index, 1);
  }

  updateItemPrice(index: number): void {
    const item = this.editForm.items[index];
    item.price = item.quantity * item.unitPrice;
  }

  confirmAll(): void {
    this.router.navigate(['/transactions']);
  }
}
