import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  InventorySummary,
  InventoryTransaction,
  InventoryTransactionPayload,
  InventoryTransactionType,
  LowStockProduct,
} from '../models/inventory.models';

export interface InventoryListFilters {
  search?: string;
  type?: InventoryTransactionType | '';
  productId?: number | null;
  product?: string;
  reference?: string;
  createdBy?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/inventory`;

  list(
    searchOrFilters: string | InventoryListFilters = '',
    type: InventoryTransactionType | '' = '',
    productId?: number | null,
    page = 0,
    size = 20,
  ): Observable<PageResponse<InventoryTransaction>> {
    const f: InventoryListFilters =
      typeof searchOrFilters === 'string'
        ? { search: searchOrFilters, type, productId, page, size }
        : { page: 0, size: 20, ...searchOrFilters };

    let params = new HttpParams().set('page', f.page ?? 0).set('size', f.size ?? 20);
    const set = (key: string, value: string | number | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', f.search);
    set('type', f.type);
    set('productId', f.productId);
    set('product', f.product);
    set('reference', f.reference);
    set('createdBy', f.createdBy);
    set('from', f.from);
    set('to', f.to);
    return this.http.get<PageResponse<InventoryTransaction>>(`${this.base}/transactions`, { params });
  }

  create(payload: InventoryTransactionPayload): Observable<InventoryTransaction> {
    return this.http.post<InventoryTransaction>(`${this.base}/transactions`, payload);
  }

  lowStock(page = 0, size = 50): Observable<PageResponse<LowStockProduct>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<LowStockProduct>>(`${this.base}/low-stock`, { params });
  }

  summary(): Observable<InventorySummary> {
    return this.http.get<InventorySummary>(`${this.base}/summary`);
  }
}
