export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'ON_HOLD';

export interface Customer {
  id: number;
  customerCode: string;
  businessName: string;
  contactPerson?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  city?: string | null;
  province?: string | null;
  taxIdentificationNumber?: string | null;
  notes?: string | null;
  creditLimit: number;
  outstandingBalance: number;
  priceLevelId?: number | null;
  priceLevelCode?: string | null;
  priceLevelName?: string | null;
  status: CustomerStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerPayload {
  customerCode: string;
  businessName: string;
  contactPerson?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  city?: string | null;
  province?: string | null;
  taxIdentificationNumber?: string | null;
  notes?: string | null;
  creditLimit: number;
  priceLevelId?: number | null;
  status: CustomerStatus;
}

export interface CustomerPurchaseHistoryItem {
  saleId: number;
  receiptNumber: string;
  soldAt: string;
  totalAmount: number;
  paymentMethod: string;
  status: string;
}
