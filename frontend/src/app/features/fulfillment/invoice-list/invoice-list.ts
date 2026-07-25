import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { FulfillmentInvoice, FulfillmentInvoiceStatus } from '../../../core/models/fulfillment.models';
import { statusClass as toStatusClass, statusLabel as toStatusLabel } from '../../../core/util/status-label';

@Component({
  selector: 'app-invoice-list',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './invoice-list.html',
  styleUrl: './invoice-list.scss',
})
export class InvoiceListComponent implements OnInit {
  private readonly api = inject(FulfillmentApiService);
  private readonly router = inject(Router);

  readonly items = signal<FulfillmentInvoice[]>([]);
  readonly error = signal<string | null>(null);
  readonly search = new FormControl('', { nonNullable: true });
  readonly status = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    merge(this.search.valueChanges, this.status.valueChanges)
      .pipe(debounceTime(300))
      .subscribe(() => this.load());

    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        if (e.urlAfterRedirects.startsWith('/fulfillment/invoices')) {
          this.load();
        }
      });
  }

  clearFilters(): void {
    this.search.setValue('');
    this.status.setValue('');
  }

  load(): void {
    this.error.set(null);
    this.api
      .listInvoices({
        search: this.search.value,
        status: this.status.value as FulfillmentInvoiceStatus | '',
        size: 50,
      })
      .subscribe({
        next: (page) => this.items.set(page.content),
        error: (err) => {
          this.items.set([]);
          this.error.set(err?.error?.message || 'Unable to load invoices');
        },
      });
  }

  statusClass(status: FulfillmentInvoiceStatus): string {
    return toStatusClass(status);
  }

  statusLabel(status: FulfillmentInvoiceStatus): string {
    return toStatusLabel(status);
  }
}
