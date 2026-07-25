import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { SalesApiService } from '../../../core/services/sales-api.service';
import { PaymentMethod, Sale, SaleStatus } from '../../../core/models/sales.models';
import { dayEndExclusiveIso, dayStartIso } from '../../../core/util/date-filters';

@Component({
  selector: 'app-sale-list',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './sale-list.html',
  styleUrl: './sale-list.scss',
})
export class SaleListComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly router = inject(Router);

  readonly items = signal<Sale[]>([]);
  readonly error = signal<string | null>(null);
  readonly receipt = new FormControl('', { nonNullable: true });
  readonly soldFrom = new FormControl('', { nonNullable: true });
  readonly soldTo = new FormControl('', { nonNullable: true });
  readonly customer = new FormControl('', { nonNullable: true });
  readonly paymentMethod = new FormControl('', { nonNullable: true });
  readonly totalMin = new FormControl('', { nonNullable: true });
  readonly totalMax = new FormControl('', { nonNullable: true });
  readonly cashier = new FormControl('', { nonNullable: true });
  readonly status = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    merge(
      this.receipt.valueChanges,
      this.soldFrom.valueChanges,
      this.soldTo.valueChanges,
      this.customer.valueChanges,
      this.paymentMethod.valueChanges,
      this.totalMin.valueChanges,
      this.totalMax.valueChanges,
      this.cashier.valueChanges,
      this.status.valueChanges,
    )
      .pipe(debounceTime(300))
      .subscribe(() => this.load());

    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        if (e.urlAfterRedirects === '/sales' || e.urlAfterRedirects.startsWith('/sales?')) {
          this.load();
        }
      });
  }

  clearFilters(): void {
    this.receipt.setValue('');
    this.soldFrom.setValue('');
    this.soldTo.setValue('');
    this.customer.setValue('');
    this.paymentMethod.setValue('');
    this.totalMin.setValue('');
    this.totalMax.setValue('');
    this.cashier.setValue('');
    this.status.setValue('');
  }

  load(): void {
    const min = this.totalMin.value.trim() ? Number(this.totalMin.value) : null;
    const max = this.totalMax.value.trim() ? Number(this.totalMax.value) : null;
    this.error.set(null);
    this.api
      .list({
        receipt: this.receipt.value,
        customer: this.customer.value,
        cashier: this.cashier.value,
        paymentMethod: this.paymentMethod.value as PaymentMethod | '',
        status: this.status.value as SaleStatus | '',
        soldFrom: dayStartIso(this.soldFrom.value),
        soldTo: dayEndExclusiveIso(this.soldTo.value),
        totalMin: min,
        totalMax: max,
        size: 50,
      })
      .subscribe({
        next: (page) => this.items.set(page.content),
        error: (err) => {
          this.items.set([]);
          this.error.set(err?.error?.message || 'Unable to load sales');
        },
      });
  }
}
