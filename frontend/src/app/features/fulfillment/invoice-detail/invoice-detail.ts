import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { STORE_PROFILE } from '../../../core/config/store-profile';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { CollectionPaymentMethod } from '../../../core/models/credit.models';
import { FulfillmentInvoice, FulfillmentInvoiceItem } from '../../../core/models/fulfillment.models';

@Component({
  selector: 'app-invoice-detail',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './invoice-detail.html',
  styleUrl: './invoice-detail.scss',
})
export class InvoiceDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(FulfillmentApiService);

  readonly store = { ...STORE_PROFILE, documentTitle: 'SALES INVOICE' };
  readonly invoice = signal<FulfillmentInvoice | null>(null);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);

  readonly paymentMethods: CollectionPaymentMethod[] = [
    'CASH',
    'CARD',
    'BANK_TRANSFER',
    'CHECK',
    'GCASH',
    'OTHER',
  ];

  readonly paymentForm = this.fb.group({
    amount: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0.01)]),
    paymentMethod: this.fb.nonNullable.control<CollectionPaymentMethod>('CASH', Validators.required),
    referenceNo: this.fb.nonNullable.control(''),
    notes: this.fb.nonNullable.control(''),
  });

  readonly tableRows = computed(() => {
    const items = this.invoice()?.items ?? [];
    const minRows = 10;
    const rows: Array<FulfillmentInvoiceItem | null> = [...items];
    while (rows.length < minRows) {
      rows.push(null);
    }
    return rows;
  });

  readonly canRecordPayment = computed(() => {
    const inv = this.invoice();
    return inv && inv.paymentMethod === 'CREDIT' && inv.amountDue > 0;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getInvoice(id).subscribe({
      next: (inv) => {
        this.invoice.set(inv);
        if (inv.amountDue > 0) {
          this.paymentForm.patchValue({ amount: inv.amountDue });
        }
      },
      error: () => this.error.set('Invoice not found'),
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

  recordPayment(): void {
    if (this.paymentForm.invalid || !this.invoice()) {
      return;
    }
    const v = this.paymentForm.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .recordInvoicePayment(this.invoice()!.id, {
        amount: v.amount,
        paymentMethod: v.paymentMethod,
        referenceNo: v.referenceNo || null,
        notes: v.notes || null,
      })
      .subscribe({
        next: (inv) => {
          this.invoice.set(inv);
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(err?.error?.message || 'Payment failed');
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
