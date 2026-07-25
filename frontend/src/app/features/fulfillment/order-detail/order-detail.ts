import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { SalesOrder, SalesOrderItem } from '../../../core/models/fulfillment.models';
import { PaymentMethod } from '../../../core/models/sales.models';
import { statusLabel } from '../../../core/util/status-label';

interface DeliveryQtyRow {
  salesOrderItemId: number;
  productSku: string;
  productName: string;
  quantityOpen: number;
  deliverQty: number;
}

@Component({
  selector: 'app-order-detail',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(FulfillmentApiService);

  readonly order = signal<SalesOrder | null>(null);
  readonly error = signal<string | null>(null);
  readonly savingDelivery = signal(false);
  readonly savingInvoice = signal(false);
  readonly deliveryRows = signal<DeliveryQtyRow[]>([]);
  readonly showInvoiceForm = signal(false);
  readonly statusLabel = statusLabel;

  readonly deliveryNotes = this.fb.nonNullable.control('');
  readonly invoiceForm = this.fb.group({
    paymentMethod: this.fb.nonNullable.control<PaymentMethod>('CASH', Validators.required),
    discountAmount: this.fb.nonNullable.control(0, [Validators.min(0)]),
    taxAmount: this.fb.nonNullable.control(0, [Validators.min(0)]),
    notes: this.fb.nonNullable.control(''),
  });

  readonly canCancel = computed(() => {
    const o = this.order();
    return o && o.status !== 'CANCELLED' && o.deliveries.length === 0;
  });

  readonly hasOpenLines = computed(() =>
    (this.order()?.items ?? []).some((i) => i.quantityOpen > 0),
  );

  readonly hasBillableLines = computed(() =>
    (this.order()?.items ?? []).some((i) => i.quantityBillable > 0),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getOrder(id).subscribe({
      next: (o) => {
        this.order.set(o);
        this.deliveryRows.set(
          o.items
            .filter((i) => i.quantityOpen > 0)
            .map((i) => ({
              salesOrderItemId: i.id,
              productSku: i.productSku,
              productName: i.productName,
              quantityOpen: i.quantityOpen,
              deliverQty: 0,
            })),
        );
        this.invoiceForm.patchValue({
          discountAmount: o.discountAmount,
          taxAmount: o.taxAmount,
        });
      },
      error: () => this.error.set('Sales order not found'),
    });
  }

  setDeliverQty(itemId: number, qty: number): void {
    const value = Math.max(0, Number(qty) || 0);
    this.deliveryRows.update((rows) =>
      rows.map((r) => {
        if (r.salesOrderItemId !== itemId) {
          return r;
        }
        return { ...r, deliverQty: Math.min(value, r.quantityOpen) };
      }),
    );
  }

  postDelivery(): void {
    const items = this.deliveryRows()
      .filter((r) => r.deliverQty > 0)
      .map((r) => ({ salesOrderItemId: r.salesOrderItemId, quantity: r.deliverQty }));
    if (items.length === 0) {
      this.error.set('Enter quantity for at least one open line');
      return;
    }

    this.savingDelivery.set(true);
    this.error.set(null);
    this.api
      .createDelivery(this.order()!.id, {
        notes: this.deliveryNotes.value || null,
        items,
      })
      .subscribe({
        next: (dr) => void this.router.navigate(['/fulfillment/deliveries', dr.id]),
        error: (err) => {
          this.savingDelivery.set(false);
          this.error.set(err?.error?.message || 'Delivery failed');
        },
      });
  }

  toggleInvoiceForm(): void {
    this.showInvoiceForm.update((v) => !v);
  }

  createInvoice(): void {
    if (this.invoiceForm.invalid) {
      return;
    }
    const v = this.invoiceForm.getRawValue();
    this.savingInvoice.set(true);
    this.error.set(null);
    this.api
      .createInvoice(this.order()!.id, {
        paymentMethod: v.paymentMethod,
        discountAmount: v.discountAmount,
        taxAmount: v.taxAmount,
        notes: v.notes || null,
      })
      .subscribe({
        next: (inv) => void this.router.navigate(['/fulfillment/invoices', inv.id]),
        error: (err) => {
          this.savingInvoice.set(false);
          this.error.set(err?.error?.message || 'Invoice failed');
        },
      });
  }

  cancelOrder(): void {
    if (!confirm('Cancel this sales order?')) {
      return;
    }
    this.error.set(null);
    this.api.cancelOrder(this.order()!.id).subscribe({
      next: (o) => this.order.set(o),
      error: (err) => this.error.set(err?.error?.message || 'Cancel failed'),
    });
  }

  lineLabel(item: SalesOrderItem): string {
    return `${item.productSku} — ${item.productName}`;
  }
}
