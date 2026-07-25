import { CollectionPaymentMethod } from './credit.models';
import { PaymentMethod } from './sales.models';

export type QuotationStatus =
  | 'DRAFT'
  | 'SENT'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CONVERTED'
  | 'CANCELLED';

export type SalesOrderStatus =
  | 'OPEN'
  | 'PARTIALLY_DELIVERED'
  | 'FULLY_DELIVERED'
  | 'CANCELLED';

export type DeliveryReceiptStatus = 'POSTED';

export type FulfillmentInvoiceStatus = 'OPEN' | 'PARTIALLY_PAID' | 'PAID' | 'VOIDED';

export interface FulfillmentSummary {
  pendingQuotes: number;
  openOrders: number;
  partialDeliveries: number;
}

export interface QuotationItem {
  id: number;
  lineNo: number;
  productId: number;
  productSku: string;
  productName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  lineDiscount: number;
  lineTotal: number;
}

export interface Quotation {
  id: number;
  quoteNumber: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  status: QuotationStatus;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string | null;
  validUntil?: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  items: QuotationItem[];
}

export interface SalesOrderItem {
  id: number;
  lineNo: number;
  productId: number;
  productSku: string;
  productName: string;
  unit: string;
  quantityOrdered: number;
  quantityDelivered: number;
  quantityInvoiced: number;
  quantityOpen: number;
  quantityBillable: number;
  unitPrice: number;
  lineDiscount: number;
  lineTotal: number;
}

export interface DeliveryReceiptSummary {
  id: number;
  drNumber: string;
  status: DeliveryReceiptStatus;
  deliveredAt: string;
}

export interface InvoiceSummary {
  id: number;
  invoiceNumber: string;
  status: FulfillmentInvoiceStatus;
  paymentMethod: PaymentMethod;
  totalAmount: number;
  amountPaid: number;
  invoicedAt: string;
}

export interface SalesOrder {
  id: number;
  soNumber: string;
  quotationId?: number | null;
  quoteNumber?: string | null;
  customerId: number;
  customerCode: string;
  customerName: string;
  status: SalesOrderStatus;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  items: SalesOrderItem[];
  deliveries: DeliveryReceiptSummary[];
  invoices: InvoiceSummary[];
}

export interface DeliveryReceiptItem {
  id: number;
  lineNo: number;
  salesOrderItemId: number;
  productId: number;
  productSku: string;
  productName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
}

export interface DeliveryReceipt {
  id: number;
  drNumber: string;
  salesOrderId: number;
  soNumber: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  status: DeliveryReceiptStatus;
  notes?: string | null;
  deliveredAt: string;
  createdBy?: string | null;
  items: DeliveryReceiptItem[];
}

export interface FulfillmentInvoiceItem {
  id: number;
  lineNo: number;
  salesOrderItemId: number;
  productId: number;
  productSku: string;
  productName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  lineDiscount: number;
  lineTotal: number;
}

export interface FulfillmentInvoice {
  id: number;
  invoiceNumber: string;
  salesOrderId: number;
  soNumber: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  customerTin?: string | null;
  customerAddress?: string | null;
  customerPhone?: string | null;
  status: FulfillmentInvoiceStatus;
  paymentMethod: PaymentMethod;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  amountPaid: number;
  amountDue: number;
  notes?: string | null;
  invoicedAt: string;
  createdBy?: string | null;
  items: FulfillmentInvoiceItem[];
}

export interface FulfillmentLinePayload {
  productId: number;
  quantity: number;
  unitPrice?: number | null;
  lineDiscount: number;
}

export interface CreateQuotationPayload {
  customerId: number;
  discountAmount: number;
  taxAmount: number;
  notes?: string | null;
  validUntil?: string | null;
  items: FulfillmentLinePayload[];
}

export interface CreateSalesOrderPayload {
  customerId: number;
  quotationId?: number | null;
  discountAmount: number;
  taxAmount: number;
  notes?: string | null;
  items: FulfillmentLinePayload[];
}

export interface DeliveryLinePayload {
  salesOrderItemId: number;
  quantity: number;
}

export interface CreateDeliveryPayload {
  notes?: string | null;
  items: DeliveryLinePayload[];
}

export interface InvoiceLinePayload {
  salesOrderItemId: number;
  quantity: number;
}

export interface CreateInvoicePayload {
  paymentMethod: PaymentMethod;
  discountAmount: number;
  taxAmount: number;
  notes?: string | null;
  items?: InvoiceLinePayload[] | null;
}

export interface RecordInvoicePaymentPayload {
  amount: number;
  paymentMethod: CollectionPaymentMethod;
  referenceNo?: string | null;
  notes?: string | null;
  paidAt?: string | null;
}

export interface QuoteFormLine {
  productId: number;
  sku: string;
  name: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  lineDiscount: number;
}
