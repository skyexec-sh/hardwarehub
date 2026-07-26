import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse, UserResponse } from '../models/api.models';

export interface CreateUserPayload {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  roles: string[];
}

export interface UpdateUserPayload {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/users`;

  list(
    searchOrFilters:
      | string
      | {
          search?: string;
          username?: string;
          name?: string;
          email?: string;
          role?: string;
          active?: boolean | null;
          page?: number;
          size?: number;
        } = '',
    page = 0,
    size = 20,
  ): Observable<PageResponse<UserResponse>> {
    const f =
      typeof searchOrFilters === 'string'
        ? { search: searchOrFilters, page, size }
        : { page: 0, size: 20, ...searchOrFilters };

    let params = new HttpParams().set('page', f.page ?? 0).set('size', f.size ?? 20);
    const set = (key: string, value: string | boolean | null | undefined) => {
      if (value !== undefined && value !== null && `${value}`.trim() !== '') {
        params = params.set(key, String(value));
      }
    };
    set('search', f.search);
    set('username', f.username);
    set('name', f.name);
    set('email', f.email);
    set('role', f.role);
    if (f.active === true || f.active === false) {
      params = params.set('active', String(f.active));
    }
    return this.http.get<PageResponse<UserResponse>>(this.base, { params });
  }

  get(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/${id}`);
  }

  create(payload: CreateUserPayload): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.base, payload);
  }

  update(id: number, payload: UpdateUserPayload): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.base}/${id}`, payload);
  }

  activate(id: number): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/${id}/activate`, {});
  }

  deactivate(id: number): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/${id}/deactivate`, {});
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/reset-password`, { newPassword });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
