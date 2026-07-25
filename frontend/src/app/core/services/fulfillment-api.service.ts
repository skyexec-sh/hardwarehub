import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  CreateDeliveryPayload,
  CreateInvoicePayload,
  CreateQuotationPayload,
  CreateSalesOrderPayload,
  DeliveryReceipt,
  FulfillmentInvoice,
  FulfillmentSummary,
  FulfillmentInvoiceStatus,
  Quotation,
  QuotationStatus,
  RecordInvoicePaymentPayload,
  SalesOrder,
  SalesOrderStatus,
} from '../models/fulfillment.models';

export interface QuoteListFilters {
  search?: string;
  status?: QuotationStatus | '';
  customerId?: number | null;
  page?: number;
  size?: number;
}

export interface OrderListFilters {
  search?: string;
  status?: SalesOrderStatus | '';
  customerId?: number | null;
  page?: number;
  size?: number;
}

export interface InvoiceListFilters {
  search?: string;
  status?: FulfillmentInvoiceStatus | '';
  customerId?: number | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class FulfillmentApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/fulfillment`;

  summary(): Observable<FulfillmentSummary> {
    return this.http.get<FulfillmentSummary>(`${this.base}/summary`);
  }

  // Quotes

  listQuotes(filters: QuoteListFilters = {}): Observable<PageResponse<Quotation>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    const set = (key: string, value: string | number | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', filters.search);
    set('status', filters.status);
    set('customerId', filters.customerId);
    return this.http.get<PageResponse<Quotation>>(`${this.base}/quotes`, { params });
  }

  getQuote(id: number): Observable<Quotation> {
    return this.http.get<Quotation>(`${this.base}/quotes/${id}`);
  }

  createQuote(payload: CreateQuotationPayload): Observable<Quotation> {
    return this.http.post<Quotation>(`${this.base}/quotes`, payload);
  }

  updateQuote(id: number, payload: CreateQuotationPayload): Observable<Quotation> {
    return this.http.put<Quotation>(`${this.base}/quotes/${id}`, payload);
  }

  sendQuote(id: number): Observable<Quotation> {
    return this.http.post<Quotation>(`${this.base}/quotes/${id}/send`, {});
  }

  acceptQuote(id: number): Observable<Quotation> {
    return this.http.post<Quotation>(`${this.base}/quotes/${id}/accept`, {});
  }

  rejectQuote(id: number): Observable<Quotation> {
    return this.http.post<Quotation>(`${this.base}/quotes/${id}/reject`, {});
  }

  cancelQuote(id: number): Observable<Quotation> {
    return this.http.post<Quotation>(`${this.base}/quotes/${id}/cancel`, {});
  }

  convertQuote(id: number): Observable<SalesOrder> {
    return this.http.post<SalesOrder>(`${this.base}/quotes/${id}/convert`, {});
  }

  // Orders

  listOrders(filters: OrderListFilters = {}): Observable<PageResponse<SalesOrder>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    const set = (key: string, value: string | number | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', filters.search);
    set('status', filters.status);
    set('customerId', filters.customerId);
    return this.http.get<PageResponse<SalesOrder>>(`${this.base}/orders`, { params });
  }

  getOrder(id: number): Observable<SalesOrder> {
    return this.http.get<SalesOrder>(`${this.base}/orders/${id}`);
  }

  createOrder(payload: CreateSalesOrderPayload): Observable<SalesOrder> {
    return this.http.post<SalesOrder>(`${this.base}/orders`, payload);
  }

  cancelOrder(id: number): Observable<SalesOrder> {
    return this.http.post<SalesOrder>(`${this.base}/orders/${id}/cancel`, {});
  }

  createDelivery(orderId: number, payload: CreateDeliveryPayload): Observable<DeliveryReceipt> {
    return this.http.post<DeliveryReceipt>(`${this.base}/orders/${orderId}/deliveries`, payload);
  }

  createInvoice(orderId: number, payload: CreateInvoicePayload): Observable<FulfillmentInvoice> {
    return this.http.post<FulfillmentInvoice>(`${this.base}/orders/${orderId}/invoices`, payload);
  }

  // Deliveries / invoices

  getDelivery(id: number): Observable<DeliveryReceipt> {
    return this.http.get<DeliveryReceipt>(`${this.base}/deliveries/${id}`);
  }

  listInvoices(filters: InvoiceListFilters = {}): Observable<PageResponse<FulfillmentInvoice>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    const set = (key: string, value: string | number | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', filters.search);
    set('status', filters.status);
    set('customerId', filters.customerId);
    return this.http.get<PageResponse<FulfillmentInvoice>>(`${this.base}/invoices`, { params });
  }

  getInvoice(id: number): Observable<FulfillmentInvoice> {
    return this.http.get<FulfillmentInvoice>(`${this.base}/invoices/${id}`);
  }

  recordInvoicePayment(id: number, payload: RecordInvoicePaymentPayload): Observable<FulfillmentInvoice> {
    return this.http.post<FulfillmentInvoice>(`${this.base}/invoices/${id}/payments`, payload);
  }
}
