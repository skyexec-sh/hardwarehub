import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  CreateSalePayload,
  PaymentMethod,
  Sale,
  SaleStatus,
  SalesSummary,
} from '../models/sales.models';

export interface SaleListFilters {
  search?: string;
  status?: SaleStatus | '';
  receipt?: string;
  customer?: string;
  cashier?: string;
  paymentMethod?: PaymentMethod | '';
  soldFrom?: string;
  soldTo?: string;
  totalMin?: number | null;
  totalMax?: number | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class SalesApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/sales`;

  list(filters: SaleListFilters = {}): Observable<PageResponse<Sale>> {
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
    set('receipt', filters.receipt);
    set('customer', filters.customer);
    set('cashier', filters.cashier);
    set('paymentMethod', filters.paymentMethod);
    set('soldFrom', filters.soldFrom);
    set('soldTo', filters.soldTo);
    set('totalMin', filters.totalMin);
    set('totalMax', filters.totalMax);
    return this.http.get<PageResponse<Sale>>(this.base, { params });
  }

  get(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.base}/${id}`);
  }

  getByReceipt(receiptNumber: string): Observable<Sale> {
    return this.http.get<Sale>(`${this.base}/receipt/${encodeURIComponent(receiptNumber)}`);
  }

  checkout(payload: CreateSalePayload): Observable<Sale> {
    return this.http.post<Sale>(this.base, payload);
  }

  summary(): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.base}/summary`);
  }
}
