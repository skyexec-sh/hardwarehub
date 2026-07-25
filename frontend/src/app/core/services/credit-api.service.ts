import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/api.models';
import {
  CreditSummary,
  CustomerPayment,
  LedgerEntry,
  RecordPaymentPayload,
  StatementOfAccount,
} from '../models/credit.models';

@Injectable({ providedIn: 'root' })
export class CreditApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  summary(): Observable<CreditSummary> {
    return this.http.get<CreditSummary>(`${this.base}/credit/summary`);
  }

  listPayments(customerId: number, page = 0, size = 50): Observable<PageResponse<CustomerPayment>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<CustomerPayment>>(`${this.base}/customers/${customerId}/payments`, {
      params,
    });
  }

  recordPayment(customerId: number, payload: RecordPaymentPayload): Observable<CustomerPayment> {
    return this.http.post<CustomerPayment>(`${this.base}/customers/${customerId}/payments`, payload);
  }

  ledger(customerId: number): Observable<LedgerEntry[]> {
    return this.http.get<LedgerEntry[]>(`${this.base}/customers/${customerId}/ledger`);
  }

  statement(customerId: number, from?: string | null, to?: string | null): Observable<StatementOfAccount> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<StatementOfAccount>(`${this.base}/customers/${customerId}/statement`, { params });
  }
}
