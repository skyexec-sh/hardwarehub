import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { STORE_PROFILE } from '../../../core/config/store-profile';
import { SalesApiService } from '../../../core/services/sales-api.service';
import { Sale, SaleItem } from '../../../core/models/sales.models';

@Component({
  selector: 'app-sale-receipt',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './sale-receipt.html',
  styleUrl: './sale-receipt.scss',
})
export class SaleReceiptComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(SalesApiService);

  readonly store = STORE_PROFILE;
  readonly sale = signal<Sale | null>(null);
  readonly error = signal<string | null>(null);

  /** Pad blank rows so the printed form looks like a paper booklet invoice. */
  readonly tableRows = computed(() => {
    const items = this.sale()?.items ?? [];
    const minRows = 10;
    const rows: Array<SaleItem | null> = [...items];
    while (rows.length < minRows) {
      rows.push(null);
    }
    return rows;
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.get(id).subscribe({
      next: (sale) => this.sale.set(sale),
      error: () => this.error.set('Sale not found'),
    });
  }

  termsLabel(method: string): string {
    switch (method) {
      case 'CREDIT':
        return 'Charge / Credit';
      case 'CARD':
        return 'Card';
      default:
        return 'Cash';
    }
  }

  print(): void {
    document.body.classList.add('printing-receipt');
    const cleanup = () => {
      document.body.classList.remove('printing-receipt');
      window.removeEventListener('afterprint', cleanup);
    };
    window.addEventListener('afterprint', cleanup);
    window.print();
    window.setTimeout(cleanup, 1500);
  }
}
