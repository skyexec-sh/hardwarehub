import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  LevelPrice,
  PriceLevel,
  ProductPriceHistoryItem,
  ResolvedPrice,
} from '../models/pricing.models';

@Injectable({ providedIn: 'root' })
export class PricingApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  listLevels(activeOnly = false): Observable<PriceLevel[]> {
    const params = new HttpParams().set('activeOnly', String(activeOnly));
    return this.http.get<PriceLevel[]>(`${this.base}/price-levels`, { params });
  }

  updateLevel(
    id: number,
    payload: { name: string; description?: string | null; active?: boolean },
  ): Observable<PriceLevel> {
    return this.http.put<PriceLevel>(`${this.base}/price-levels/${id}`, payload);
  }

  productLevelPrices(productId: number): Observable<LevelPrice[]> {
    return this.http.get<LevelPrice[]>(`${this.base}/products/${productId}/level-prices`);
  }

  priceHistory(productId: number, page = 0, size = 50): Observable<PageResponse<ProductPriceHistoryItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<ProductPriceHistoryItem>>(
      `${this.base}/products/${productId}/price-history`,
      { params },
    );
  }

  resolve(productId: number, customerId?: number | null, priceLevelId?: number | null): Observable<ResolvedPrice> {
    let params = new HttpParams().set('productId', productId);
    if (customerId) {
      params = params.set('customerId', customerId);
    }
    if (priceLevelId) {
      params = params.set('priceLevelId', priceLevelId);
    }
    return this.http.get<ResolvedPrice>(`${this.base}/pricing/resolve`, { params });
  }
}
