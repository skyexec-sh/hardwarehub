import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { STORE_PROFILE } from '../../../core/config/store-profile';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { DeliveryReceipt, DeliveryReceiptItem } from '../../../core/models/fulfillment.models';

@Component({
  selector: 'app-delivery-detail',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './delivery-detail.html',
  styleUrl: './delivery-detail.scss',
})
export class DeliveryDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(FulfillmentApiService);

  readonly store = { ...STORE_PROFILE, documentTitle: 'DELIVERY RECEIPT' };
  readonly delivery = signal<DeliveryReceipt | null>(null);
  readonly error = signal<string | null>(null);

  readonly tableRows = computed(() => {
    const items = this.delivery()?.items ?? [];
    const minRows = 10;
    const rows: Array<DeliveryReceiptItem | null> = [...items];
    while (rows.length < minRows) {
      rows.push(null);
    }
    return rows;
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getDelivery(id).subscribe({
      next: (d) => this.delivery.set(d),
      error: () => this.error.set('Delivery receipt not found'),
    });
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
