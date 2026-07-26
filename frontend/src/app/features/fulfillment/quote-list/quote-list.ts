import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { Quotation, QuotationStatus } from '../../../core/models/fulfillment.models';
import { statusClass as toStatusClass, statusLabel as toStatusLabel } from '../../../core/util/status-label';

@Component({
  selector: 'app-quote-list',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './quote-list.html',
  styleUrl: './quote-list.scss',
})
export class QuoteListComponent implements OnInit {
  private readonly api = inject(FulfillmentApiService);
  private readonly router = inject(Router);

  readonly items = signal<Quotation[]>([]);
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
        if (e.urlAfterRedirects.startsWith('/fulfillment/quotes')) {
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
      .listQuotes({
        search: this.search.value,
        status: this.status.value as QuotationStatus | '',
        size: 50,
      })
      .subscribe({
        next: (page) => this.items.set(page.content),
        error: (err) => {
          this.items.set([]);
          this.error.set(err?.error?.message || 'Unable to load quotations');
        },
      });
  }

  statusClass(status: QuotationStatus): string {
    return toStatusClass(status);
  }

  statusLabel(status: QuotationStatus): string {
    return toStatusLabel(status);
  }
}
