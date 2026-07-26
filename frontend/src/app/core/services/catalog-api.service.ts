import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  Brand,
  BrandPayload,
  Category,
  CategoryPayload,
  Product,
  ProductPayload,
} from '../models/catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  listCategories(search = '', page = 0, size = 50): Observable<PageResponse<Category>> {
    const params = new HttpParams().set('search', search).set('page', page).set('size', size);
    return this.http.get<PageResponse<Category>>(`${this.base}/categories`, { params });
  }

  activeCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories/active`);
  }

  getCategory(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.base}/categories/${id}`);
  }

  createCategory(payload: CategoryPayload): Observable<Category> {
    return this.http.post<Category>(`${this.base}/categories`, payload);
  }

  updateCategory(id: number, payload: CategoryPayload): Observable<Category> {
    return this.http.put<Category>(`${this.base}/categories/${id}`, payload);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/categories/${id}`);
  }

  listBrands(search = '', page = 0, size = 50): Observable<PageResponse<Brand>> {
    const params = new HttpParams().set('search', search).set('page', page).set('size', size);
    return this.http.get<PageResponse<Brand>>(`${this.base}/brands`, { params });
  }

  activeBrands(): Observable<Brand[]> {
    return this.http.get<Brand[]>(`${this.base}/brands/active`);
  }

  getBrand(id: number): Observable<Brand> {
    return this.http.get<Brand>(`${this.base}/brands/${id}`);
  }

  createBrand(payload: BrandPayload): Observable<Brand> {
    return this.http.post<Brand>(`${this.base}/brands`, payload);
  }

  updateBrand(id: number, payload: BrandPayload): Observable<Brand> {
    return this.http.put<Brand>(`${this.base}/brands/${id}`, payload);
  }

  deleteBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/brands/${id}`);
  }

  listProducts(
    searchOrFilters:
      | string
      | {
          search?: string;
          categoryId?: number | null;
          brandId?: number | null;
          sku?: string;
          name?: string;
          active?: boolean | null;
          lowStockOnly?: boolean | null;
          page?: number;
          size?: number;
        } = '',
    categoryId?: number | null,
    brandId?: number | null,
    page = 0,
    size = 20,
  ): Observable<PageResponse<Product>> {
    const f =
      typeof searchOrFilters === 'string'
        ? { search: searchOrFilters, categoryId, brandId, page, size }
        : { page: 0, size: 20, ...searchOrFilters };

    let params = new HttpParams().set('page', f.page ?? 0).set('size', f.size ?? 20);
    const set = (key: string, value: string | number | boolean | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', f.search);
    set('categoryId', f.categoryId);
    set('brandId', f.brandId);
    set('sku', f.sku);
    set('name', f.name);
    if (f.active === true || f.active === false) {
      params = params.set('active', String(f.active));
    }
    if (f.lowStockOnly === true) {
      params = params.set('lowStockOnly', 'true');
    }
    return this.http.get<PageResponse<Product>>(`${this.base}/products`, { params });
  }

  getProduct(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.base}/products/${id}`);
  }

  getProductByBarcode(barcode: string): Observable<Product> {
    return this.http.get<Product>(`${this.base}/products/barcode/${encodeURIComponent(barcode)}`);
  }

  createProduct(payload: ProductPayload): Observable<Product> {
    return this.http.post<Product>(`${this.base}/products`, payload);
  }

  updateProduct(id: number, payload: ProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.base}/products/${id}`, payload);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/products/${id}`);
  }
}
