import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { InventoryApiService } from '../../../core/services/inventory-api.service';
import { LowStockProduct } from '../../../core/models/inventory.models';

@Component({
  selector: 'app-inventory-low-stock',
  imports: [RouterLink],
  templateUrl: './inventory-low-stock.html',
  styleUrl: '../inventory-history/inventory-history.scss',
})
export class InventoryLowStockComponent implements OnInit {
  private readonly api = inject(InventoryApiService);

  readonly items = signal<LowStockProduct[]>([]);

  ngOnInit(): void {
    this.api.lowStock().subscribe({
      next: (page) => this.items.set(page.content),
    });
  }
}
