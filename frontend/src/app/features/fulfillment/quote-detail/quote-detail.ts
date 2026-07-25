import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { STORE_PROFILE } from '../../../core/config/store-profile';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { Quotation, QuotationItem } from '../../../core/models/fulfillment.models';
import { statusLabel } from '../../../core/util/status-label';

@Component({
  selector: 'app-quote-detail',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './quote-detail.html',
  styleUrl: './quote-detail.scss',
})
export class QuoteDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(FulfillmentApiService);

  readonly store = { ...STORE_PROFILE, documentTitle: 'QUOTATION' };
  readonly quote = signal<Quotation | null>(null);
  readonly error = signal<string | null>(null);
  readonly acting = signal(false);
  readonly statusLabel = statusLabel;

  readonly tableRows = computed(() => {
    const items = this.quote()?.items ?? [];
    const minRows = 10;
    const rows: Array<QuotationItem | null> = [...items];
    while (rows.length < minRows) {
      rows.push(null);
    }
    return rows;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getQuote(id).subscribe({
      next: (q) => this.quote.set(q),
      error: () => this.error.set('Quotation not found'),
    });
  }

  canSend(q: Quotation): boolean {
    return q.status === 'DRAFT';
  }

  canAccept(q: Quotation): boolean {
    return q.status === 'SENT';
  }

  canReject(q: Quotation): boolean {
    return q.status === 'SENT';
  }

  canCancel(q: Quotation): boolean {
    return q.status === 'DRAFT' || q.status === 'SENT' || q.status === 'ACCEPTED';
  }

  canConvert(q: Quotation): boolean {
    return q.status === 'SENT' || q.status === 'ACCEPTED';
  }

  send(): void {
    this.act('send', () => this.api.sendQuote(this.quote()!.id));
  }

  accept(): void {
    this.act('accept', () => this.api.acceptQuote(this.quote()!.id));
  }

  reject(): void {
    this.act('reject', () => this.api.rejectQuote(this.quote()!.id));
  }

  cancel(): void {
    if (!confirm('Cancel this quotation?')) {
      return;
    }
    this.act('cancel', () => this.api.cancelQuote(this.quote()!.id));
  }

  convert(): void {
    this.acting.set(true);
    this.error.set(null);
    this.api.convertQuote(this.quote()!.id).subscribe({
      next: (order) => void this.router.navigate(['/fulfillment/orders', order.id]),
      error: (err) => {
        this.acting.set(false);
        this.error.set(err?.error?.message || 'Convert failed');
      },
    });
  }

  private act(_label: string, fn: () => ReturnType<FulfillmentApiService['sendQuote']>): void {
    this.acting.set(true);
    this.error.set(null);
    fn().subscribe({
      next: (q) => {
        this.quote.set(q);
        this.acting.set(false);
      },
      error: (err) => {
        this.acting.set(false);
        this.error.set(err?.error?.message || 'Action failed');
      },
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
