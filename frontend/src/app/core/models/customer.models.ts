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
  status: CustomerStatus;
}

export interface CustomerPurchaseHistoryItem {
  saleId: number;
  receiptNumber: string;
  soldAt: string;
  totalAmount: number;
  status: string;
}
