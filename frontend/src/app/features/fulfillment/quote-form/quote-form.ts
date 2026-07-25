import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { CustomerApiService } from '../../../core/services/customer-api.service';
import { FulfillmentApiService } from '../../../core/services/fulfillment-api.service';
import { PricingApiService } from '../../../core/services/pricing-api.service';
import { Product } from '../../../core/models/catalog.models';
import { Customer } from '../../../core/models/customer.models';
import { QuoteFormLine } from '../../../core/models/fulfillment.models';

@Component({
  selector: 'app-quote-form',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './quote-form.html',
  styleUrl: './quote-form.scss',
})
export class QuoteFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogApi = inject(CatalogApiService);
  private readonly customerApi = inject(CustomerApiService);
  private readonly fulfillmentApi = inject(FulfillmentApiService);
  private readonly pricingApi = inject(PricingApiService);

  readonly lines = signal<QuoteFormLine[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly searchHits = signal<Product[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly quoteId = signal<number | null>(null);
  readonly isEdit = computed(() => this.quoteId() !== null);

  readonly productSearch = new FormControl('', { nonNullable: true });
  readonly customerId = new FormControl('', { nonNullable: true });
  readonly discountAmount = new FormControl(0, { nonNullable: true });
  readonly taxAmount = new FormControl(0, { nonNullable: true });
  readonly notes = new FormControl('', { nonNullable: true });
  readonly validUntil = new FormControl('', { nonNullable: true });

  readonly subtotal = computed(() =>
    this.lines().reduce((sum, l) => sum + l.quantity * l.unitPrice - l.lineDiscount, 0),
  );
  readonly total = computed(() =>
    Math.max(0, this.subtotal() - Number(this.discountAmount.value || 0) + Number(this.taxAmount.value || 0)),
  );

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.quoteId.set(id);
      this.loadQuote(id);
    }

    this.customerApi.list('', 'ACTIVE', 0, 200).subscribe({
      next: (page) => this.customers.set(page.content),
    });
  }

  private loadQuote(id: number): void {
    this.fulfillmentApi.getQuote(id).subscribe({
      next: (q) => {
        if (q.status !== 'DRAFT') {
          this.error.set('Only draft quotations can be edited');
          return;
        }
        this.customerId.setValue(String(q.customerId));
        this.discountAmount.setValue(q.discountAmount);
        this.taxAmount.setValue(q.taxAmount);
        this.notes.setValue(q.notes || '');
        this.validUntil.setValue(q.validUntil || '');
        this.lines.set(
          q.items.map((i) => ({
            productId: i.productId,
            sku: i.productSku,
            name: i.productName,
            unit: i.unit,
            quantity: i.quantity,
            unitPrice: i.unitPrice,
            lineDiscount: i.lineDiscount,
          })),
        );
      },
      error: () => this.error.set('Quotation not found'),
    });
  }

  searchProducts(): void {
    const q = this.productSearch.value.trim();
    if (!q) {
      this.searchHits.set([]);
      return;
    }
    this.catalogApi.listProducts(q, null, null, 0, 10).subscribe({
      next: (page) => this.searchHits.set(page.content),
    });
  }

  addProduct(product: Product): void {
    this.error.set(null);
    if (!product.active) {
      this.error.set('Product is inactive');
      return;
    }
    const existing = this.lines().find((l) => l.productId === product.id);
    if (existing) {
      this.lines.update((rows) =>
        rows.map((l) => (l.productId === product.id ? { ...l, quantity: l.quantity + 1 } : l)),
      );
      this.searchHits.set([]);
      this.productSearch.setValue('');
      return;
    }

    const customerId = this.customerId.value ? Number(this.customerId.value) : null;
    this.pricingApi.resolve(product.id, customerId).subscribe({
      next: (resolved) => {
        this.lines.update((rows) => [
          ...rows,
          {
            productId: product.id,
            sku: product.sku,
            name: product.name,
            unit: product.unit,
            quantity: 1,
            unitPrice: resolved.unitPrice,
            lineDiscount: 0,
          },
        ]);
        this.searchHits.set([]);
        this.productSearch.setValue('');
      },
      error: () => {
        this.lines.update((rows) => [
          ...rows,
          {
            productId: product.id,
            sku: product.sku,
            name: product.name,
            unit: product.unit,
            quantity: 1,
            unitPrice: product.sellingPrice,
            lineDiscount: 0,
          },
        ]);
        this.searchHits.set([]);
        this.productSearch.setValue('');
      },
    });
  }

  setQty(productId: number, quantity: number): void {
    const qty = Number(quantity);
    this.lines.update((rows) =>
      rows
        .map((l) => (l.productId === productId ? { ...l, quantity: Math.max(0.001, qty) } : l))
        .filter((l) => l.quantity > 0),
    );
  }

  setUnitPrice(productId: number, unitPrice: number): void {
    const price = Math.max(0, Number(unitPrice) || 0);
    this.lines.update((rows) =>
      rows.map((l) => (l.productId === productId ? { ...l, unitPrice: price } : l)),
    );
  }

  setLineDiscount(productId: number, discount: number): void {
    this.lines.update((rows) =>
      rows.map((l) =>
        l.productId === productId ? { ...l, lineDiscount: Math.max(0, Number(discount) || 0) } : l,
      ),
    );
  }

  removeLine(productId: number): void {
    this.lines.update((rows) => rows.filter((l) => l.productId !== productId));
  }

  save(): void {
    if (!this.customerId.value) {
      this.error.set('Select a customer');
      return;
    }
    if (this.lines().length === 0) {
      this.error.set('Add at least one line item');
      return;
    }

    const payload = {
      customerId: Number(this.customerId.value),
      discountAmount: Number(this.discountAmount.value || 0),
      taxAmount: Number(this.taxAmount.value || 0),
      notes: this.notes.value || null,
      validUntil: this.validUntil.value || null,
      items: this.lines().map((l) => ({
        productId: l.productId,
        quantity: l.quantity,
        unitPrice: l.unitPrice,
        lineDiscount: l.lineDiscount,
      })),
    };

    this.saving.set(true);
    this.error.set(null);
    const id = this.quoteId();
    const req = id
      ? this.fulfillmentApi.updateQuote(id, payload)
      : this.fulfillmentApi.createQuote(payload);

    req.subscribe({
      next: (q) => void this.router.navigate(['/fulfillment/quotes', q.id]),
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message || 'Unable to save quotation');
      },
    });
  }
}
