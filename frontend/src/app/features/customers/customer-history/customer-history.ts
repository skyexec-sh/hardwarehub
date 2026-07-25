import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CreditApiService } from '../../../core/services/credit-api.service';
import { CustomerApiService } from '../../../core/services/customer-api.service';
import { STORE_PROFILE } from '../../../core/config/store-profile';
import { Customer, CustomerPurchaseHistoryItem } from '../../../core/models/customer.models';
import {
  CollectionPaymentMethod,
  CustomerPayment,
  LedgerEntry,
  StatementOfAccount,
} from '../../../core/models/credit.models';

type AccountTab = 'overview' | 'ledger' | 'payments' | 'soa' | 'purchases';

@Component({
  selector: 'app-customer-history',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, CurrencyPipe],
  templateUrl: './customer-history.html',
  styleUrl: './customer-history.scss',
})
export class CustomerHistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly customerApi = inject(CustomerApiService);
  private readonly creditApi = inject(CreditApiService);

  readonly store = STORE_PROFILE;
  readonly customer = signal<Customer | null>(null);
  readonly history = signal<CustomerPurchaseHistoryItem[]>([]);
  readonly ledger = signal<LedgerEntry[]>([]);
  readonly payments = signal<CustomerPayment[]>([]);
  readonly statement = signal<StatementOfAccount | null>(null);
  readonly tab = signal<AccountTab>('overview');
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly customerId = signal(0);

  readonly paymentMethods: CollectionPaymentMethod[] = [
    'CASH',
    'CARD',
    'BANK_TRANSFER',
    'CHECK',
    'GCASH',
    'OTHER',
  ];

  readonly paymentForm = this.fb.group({
    amount: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0.01)]),
    paymentMethod: this.fb.nonNullable.control<CollectionPaymentMethod>('CASH', Validators.required),
    referenceNo: this.fb.nonNullable.control(''),
    notes: this.fb.nonNullable.control(''),
  });

  readonly soaForm = this.fb.group({
    from: this.fb.nonNullable.control(this.defaultFromDate()),
    to: this.fb.nonNullable.control(this.todayDate()),
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.customerId.set(id);
    this.reloadAll();

    const initialTab = this.route.snapshot.queryParamMap.get('tab') as AccountTab | null;
    if (initialTab && ['overview', 'ledger', 'payments', 'soa', 'purchases'].includes(initialTab)) {
      this.tab.set(initialTab);
    }
  }

  setTab(tab: AccountTab): void {
    this.tab.set(tab);
    this.error.set(null);
    if (tab === 'soa' && !this.statement()) {
      this.loadStatement();
    }
  }

  availableCredit(): number {
    const c = this.customer();
    if (!c) {
      return 0;
    }
    return Math.max(0, c.creditLimit - c.outstandingBalance);
  }

  submitPayment(): void {
    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }
    const c = this.customer();
    if (!c) {
      return;
    }
    const value = this.paymentForm.getRawValue();
    if (value.amount > c.outstandingBalance) {
      this.error.set(`Payment exceeds outstanding balance (₱${c.outstandingBalance})`);
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.creditApi
      .recordPayment(c.id, {
        amount: Number(value.amount),
        paymentMethod: value.paymentMethod,
        referenceNo: value.referenceNo || null,
        notes: value.notes || null,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.paymentForm.reset({
            amount: 0,
            paymentMethod: 'CASH',
            referenceNo: '',
            notes: '',
          });
          this.reloadAll();
          this.tab.set('ledger');
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(err?.error?.message || 'Unable to record payment');
        },
      });
  }

  loadStatement(): void {
    const id = this.customerId();
    const { from, to } = this.soaForm.getRawValue();
    this.error.set(null);
    this.creditApi.statement(id, this.toStartInstant(from), this.toEndInstant(to)).subscribe({
      next: (soa) => this.statement.set(soa),
      error: (err) => this.error.set(err?.error?.message || 'Unable to load statement'),
    });
  }

  printSoa(): void {
    document.body.classList.add('printing-receipt');
    window.print();
    window.setTimeout(() => document.body.classList.remove('printing-receipt'), 500);
  }

  methodLabel(method: string): string {
    return method.replaceAll('_', ' ');
  }

  private reloadAll(): void {
    const id = this.customerId();
    this.customerApi.get(id).subscribe({
      next: (c) => {
        this.customer.set(c);
        if (c.outstandingBalance > 0) {
          this.paymentForm.patchValue({ amount: c.outstandingBalance });
        }
      },
    });
    this.customerApi.purchaseHistory(id).subscribe({ next: (rows) => this.history.set(rows) });
    this.creditApi.ledger(id).subscribe({ next: (rows) => this.ledger.set(rows) });
    this.creditApi.listPayments(id).subscribe({ next: (page) => this.payments.set(page.content) });
    if (this.tab() === 'soa') {
      this.loadStatement();
    }
  }

  private todayDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private defaultFromDate(): string {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    return d.toISOString().slice(0, 10);
  }

  private toStartInstant(date: string): string {
    return `${date}T00:00:00.000+08:00`;
  }

  private toEndInstant(date: string): string {
    // Exclusive end = next day start
    const d = new Date(`${date}T00:00:00+08:00`);
    d.setDate(d.getDate() + 1);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}T00:00:00.000+08:00`;
  }
}
