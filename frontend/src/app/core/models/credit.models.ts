export type CollectionPaymentMethod =
  | 'CASH'
  | 'CARD'
  | 'BANK_TRANSFER'
  | 'CHECK'
  | 'GCASH'
  | 'OTHER';

export type LedgerEntryType = 'CHARGE' | 'PAYMENT';

export interface CustomerPayment {
  id: number;
  paymentNumber: string;
  customerId: number;
  amount: number;
  paymentMethod: CollectionPaymentMethod;
  referenceNo?: string | null;
  notes?: string | null;
  paidAt: string;
  balanceBefore: number;
  balanceAfter: number;
  createdAt: string;
  createdBy?: string | null;
}

export interface RecordPaymentPayload {
  amount: number;
  paymentMethod: CollectionPaymentMethod;
  referenceNo?: string | null;
  notes?: string | null;
  paidAt?: string | null;
}

export interface LedgerEntry {
  entryType: LedgerEntryType;
  occurredAt: string;
  reference: string;
  description: string;
  chargeAmount: number;
  paymentAmount: number;
  runningBalance: number;
  saleId?: number | null;
  paymentId?: number | null;
}

export interface StatementOfAccount {
  customerId: number;
  customerCode: string;
  businessName: string;
  contactPerson?: string | null;
  phone?: string | null;
  address?: string | null;
  city?: string | null;
  province?: string | null;
  taxIdentificationNumber?: string | null;
  periodFrom: string;
  periodTo: string;
  openingBalance: number;
  totalCharges: number;
  totalPayments: number;
  closingBalance: number;
  creditLimit: number;
  currentOutstanding: number;
  lines: LedgerEntry[];
}

export interface CreditSummary {
  customersWithBalance: number;
  totalOutstanding: number;
}
