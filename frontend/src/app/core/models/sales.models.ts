export type PaymentMethod = 'CASH' | 'CARD' | 'CREDIT';
export type SaleStatus = 'COMPLETED' | 'VOIDED';

export interface SaleItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  lineDiscount: number;
  lineTotal: number;
  lineNo: number;
}

export interface Sale {
  id: number;
  receiptNumber: string;
  customerId?: number | null;
  customerCode?: string | null;
  customerName?: string | null;
  customerTin?: string | null;
  customerAddress?: string | null;
  customerPhone?: string | null;
  cashierUsername: string;
  status: SaleStatus;
  paymentMethod: PaymentMethod;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  amountTendered?: number | null;
  changeAmount?: number | null;
  notes?: string | null;
  soldAt: string;
  items: SaleItem[];
}

export interface SaleItemPayload {
  productId: number;
  quantity: number;
  unitPrice?: number | null;
  lineDiscount: number;
}

export interface CreateSalePayload {
  customerId?: number | null;
  paymentMethod: PaymentMethod;
  discountAmount: number;
  taxAmount: number;
  amountTendered?: number | null;
  notes?: string | null;
  items: SaleItemPayload[];
}

export interface SalesSummary {
  todaySales: number;
  todayReceiptCount: number;
  monthSales: number;
  monthReceiptCount: number;
}

export interface CartLine {
  productId: number;
  sku: string;
  name: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  lineDiscount: number;
  stock: number;
}
