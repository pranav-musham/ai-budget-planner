export type PaymentMethod = 'Bank Transfer' | 'Cash' | 'Check' | 'Direct Deposit' | 'PayPal' | 'Venmo' | 'Other';

export interface IncomeSource {
  id?: number;
  sourceName: string;
  amount: number;
  transactionDate: string;
  paymentMethod?: string;
  notes?: string;
  createdAt?: string;
}

export interface CreateIncomeRequest {
  sourceName: string;
  amount: number;
  transactionDate: string;
  paymentMethod?: string;
  notes?: string;
}
