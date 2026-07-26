import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { InventoryApiService } from '../../../core/services/inventory-api.service';
import { InventoryTransaction, InventoryTransactionType } from '../../../core/models/inventory.models';
import { dayEndExclusiveIso, dayStartIso } from '../../../core/util/date-filters';

@Component({
  selector: 'app-inventory-history',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './inventory-history.html',
  styleUrl: './inventory-history.scss',
})
export class InventoryHistoryComponent implements OnInit {
  private readonly api = inject(InventoryApiService);

  readonly items = signal<InventoryTransaction[]>([]);
  readonly from = new FormControl('', { nonNullable: true });
  readonly to = new FormControl('', { nonNullable: true });
  readonly type = new FormControl('', { nonNullable: true });
  readonly product = new FormControl('', { nonNullable: true });
  readonly reference = new FormControl('', { nonNullable: true });
  readonly createdBy = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    merge(
      this.from.valueChanges,
      this.to.valueChanges,
      this.type.valueChanges,
      this.product.valueChanges,
      this.reference.valueChanges,
      this.createdBy.valueChanges,
    )
      .pipe(debounceTime(300))
      .subscribe(() => this.load());
  }

  clearFilters(): void {
    this.from.setValue('');
    this.to.setValue('');
    this.type.setValue('');
    this.product.setValue('');
    this.reference.setValue('');
    this.createdBy.setValue('');
  }

  load(): void {
    this.api
      .list({
        type: this.type.value as InventoryTransactionType | '',
        product: this.product.value,
        reference: this.reference.value,
        createdBy: this.createdBy.value,
        from: dayStartIso(this.from.value),
        to: dayEndExclusiveIso(this.to.value),
      })
      .subscribe({ next: (page) => this.items.set(page.content) });
  }

  label(type: InventoryTransactionType): string {
    switch (type) {
      case 'STOCK_IN':
        return 'Stock in';
      case 'STOCK_OUT':
        return 'Stock out';
      case 'ADJUSTMENT':
        return 'Adjustment';
    }
  }

  qtyDisplay(item: InventoryTransaction): string {
    if (item.transactionType === 'STOCK_IN') {
      return `+${item.quantity}`;
    }
    if (item.transactionType === 'STOCK_OUT') {
      return `−${item.quantity}`;
    }
    return item.quantity > 0 ? `+${item.quantity}` : `${item.quantity}`;
  }
}
