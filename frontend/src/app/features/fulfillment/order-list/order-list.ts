import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { SalesOrder, SalesOrderStatus } from '../../../core/models/fulfillment.models';
import { statusClass as toStatusClass, statusLabel as toStatusLabel } from '../../../core/util/status-label';

@Component({
  selector: 'app-order-list',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderListComponent implements OnInit {
  private readonly api = inject(FulfillmentApiService);
  private readonly router = inject(Router);

  readonly items = signal<SalesOrder[]>([]);
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
        if (e.urlAfterRedirects.startsWith('/fulfillment/orders')) {
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
      .listOrders({
        search: this.search.value,
        status: this.status.value as SalesOrderStatus | '',
        size: 50,
      })
      .subscribe({
        next: (page) => this.items.set(page.content),
        error: (err) => {
          this.items.set([]);
          this.error.set(err?.error?.message || 'Unable to load sales orders');
        },
      });
  }

  statusClass(status: SalesOrderStatus): string {
    return toStatusClass(status);
  }

  statusLabel(status: SalesOrderStatus): string {
    return toStatusLabel(status);
  }
}
