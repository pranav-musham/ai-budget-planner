export interface LineItem {
  name: string;
  quantity?: number;
  unitPrice?: number;
  price: number;
}

export interface Transaction {
  id?: number;
  userId: number;
  imageUrl?: string;
  merchantName: string;
  amount: number;
  transactionDate: string;
  category: string;
  paymentMethod?: string;
  notes?: string;
  items?: LineItem[];
  confidenceScore?: number;
  isManualEntry?: boolean;
  needsReview?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateTransactionRequest {
  merchantName: string;
  amount: number;
  transactionDate: string;
  category: string;
  paymentMethod?: string;
  notes?: string;
  items?: LineItemRequest[];
}

export interface LineItemRequest {
  name: string;
  quantity: number;
  unitPrice: number;
  price?: number;
}

export interface UpdateTransactionRequest {
  merchantName?: string;
  amount?: number;
  transactionDate?: string;
  category?: string;
  paymentMethod?: string;
  notes?: string;
}
