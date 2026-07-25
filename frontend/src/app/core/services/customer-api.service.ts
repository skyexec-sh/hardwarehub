import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  Customer,
  CustomerPayload,
  CustomerPurchaseHistoryItem,
  CustomerStatus,
} from '../models/customer.models';

export interface CustomerListFilters {
  search?: string;
  status?: CustomerStatus | '';
  code?: string;
  businessName?: string;
  contact?: string;
  phone?: string;
  city?: string;
  hasBalanceDue?: boolean | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class CustomerApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/customers`;

  list(filters: CustomerListFilters | string = {}, status: CustomerStatus | '' = '', page = 0, size = 20): Observable<PageResponse<Customer>> {
    // Back-compat for callers passing (search, status, page, size)
    const f: CustomerListFilters =
      typeof filters === 'string'
        ? { search: filters, status, page, size }
        : { page: 0, size: 20, ...filters };

    let params = new HttpParams().set('page', f.page ?? 0).set('size', f.size ?? 20);
    const set = (key: string, value: string | boolean | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', f.search);
    set('status', f.status);
    set('code', f.code);
    set('businessName', f.businessName);
    set('contact', f.contact);
    set('phone', f.phone);
    set('city', f.city);
    if (f.hasBalanceDue === true) {
      params = params.set('hasBalanceDue', 'true');
    }
    return this.http.get<PageResponse<Customer>>(this.base, { params });
  }

  get(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.base}/${id}`);
  }

  purchaseHistory(id: number): Observable<CustomerPurchaseHistoryItem[]> {
    return this.http.get<CustomerPurchaseHistoryItem[]>(`${this.base}/${id}/purchase-history`);
  }

  create(payload: CustomerPayload): Observable<Customer> {
    return this.http.post<Customer>(this.base, payload);
  }

  update(id: number, payload: CustomerPayload): Observable<Customer> {
    return this.http.put<Customer>(`${this.base}/${id}`, payload);
  }

  updateStatus(id: number, status: CustomerStatus): Observable<Customer> {
    return this.http.post<Customer>(`${this.base}/${id}/status`, { status });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
