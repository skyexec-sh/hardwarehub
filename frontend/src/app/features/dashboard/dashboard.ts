import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { InventoryApiService } from '../../core/services/inventory-api.service';
import { SalesApiService } from '../../core/services/sales-api.service';

@Component({
  selector: 'app-dashboard',
  imports: [MatIconModule, RouterLink, CurrencyPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly salesApi = inject(SalesApiService);

  readonly lowStockCount = signal<number | null>(null);
  readonly todaySales = signal<number | null>(null);
  readonly monthSales = signal<number | null>(null);

  ngOnInit(): void {
    this.inventoryApi.summary().subscribe({
      next: (s) => this.lowStockCount.set(s.lowStockCount),
      error: () => this.lowStockCount.set(null),
    });
    this.salesApi.summary().subscribe({
      next: (s) => {
        this.todaySales.set(s.todaySales);
        this.monthSales.set(s.monthSales);
      },
      error: () => {
        this.todaySales.set(null);
        this.monthSales.set(null);
      },
    });
  }
}
