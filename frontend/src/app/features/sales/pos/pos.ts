import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { CustomerApiService } from '../../../core/services/customer-api.service';
import { PricingApiService } from '../../../core/services/pricing-api.service';
import { SalesApiService } from '../../../core/services/sales-api.service';
import { Product } from '../../../core/models/catalog.models';
import { Customer } from '../../../core/models/customer.models';
import { CartLine, PaymentMethod } from '../../../core/models/sales.models';

@Component({
  selector: 'app-pos',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './pos.html',
  styleUrl: './pos.scss',
})
export class PosComponent implements OnInit {
  private readonly catalogApi = inject(CatalogApiService);
  private readonly customerApi = inject(CustomerApiService);
  private readonly salesApi = inject(SalesApiService);
  private readonly pricingApi = inject(PricingApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly cart = signal<CartLine[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly discount = signal(0);
  readonly tax = signal(0);
  readonly tendered = signal(0);
  readonly method = signal<PaymentMethod>('CASH');

  readonly productSearch = new FormControl('', { nonNullable: true });
  readonly barcode = new FormControl('', { nonNullable: true });
  readonly customerId = new FormControl('', { nonNullable: true });
  readonly paymentMethod = new FormControl<PaymentMethod>('CASH', { nonNullable: true });
  readonly discountAmount = new FormControl(0, { nonNullable: true });
  readonly taxAmount = new FormControl(0, { nonNullable: true });
  readonly amountTendered = new FormControl(0, { nonNullable: true });
  readonly notes = new FormControl('', { nonNullable: true });
  readonly cashierName = new FormControl('', { nonNullable: true });
  readonly receivedBy = new FormControl('', { nonNullable: true });

  readonly searchHits = signal<Product[]>([]);
  readonly customerIdValue = signal('');
  readonly selectedCustomer = computed(() => {
    const id = Number(this.customerIdValue() || 0);
    if (!id) {
      return null;
    }
    return this.customers().find((c) => c.id === id) ?? null;
  });
  readonly availableCredit = computed(() => {
    const c = this.selectedCustomer();
    if (!c) {
      return null;
    }
    return Math.max(0, c.creditLimit - c.outstandingBalance);
  });
  readonly activePriceLevel = computed(() => {
    const c = this.selectedCustomer();
    return c?.priceLevelName || 'Retail';
  });

  readonly subtotal = computed(() =>
    this.cart().reduce((sum, line) => sum + line.quantity * line.unitPrice - line.lineDiscount, 0),
  );
  readonly total = computed(() => Math.max(0, this.subtotal() - this.discount() + this.tax()));
  readonly change = computed(() => {
    if (this.method() !== 'CASH') {
      return 0;
    }
    return Math.max(0, this.tendered() - this.total());
  });

  ngOnInit(): void {
    const user = this.auth.user();
    if (user) {
      const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
      this.cashierName.setValue(fullName || user.username);
    }
    this.customerApi.list('', 'ACTIVE', 0, 100).subscribe({
      next: (page) => this.customers.set(page.content),
    });
    this.customerId.valueChanges.subscribe((v) => this.customerIdValue.set(v));
    this.paymentMethod.valueChanges.subscribe((m) => {
      this.method.set(m);
      if (m === 'CASH') {
        this.syncTenderedToTotal();
      }
    });
    this.discountAmount.valueChanges.subscribe((v) => {
      this.discount.set(Number(v || 0));
      this.syncTenderedToTotal();
    });
    this.taxAmount.valueChanges.subscribe((v) => {
      this.tax.set(Number(v || 0));
      this.syncTenderedToTotal();
    });
    this.amountTendered.valueChanges.subscribe((v) => this.tendered.set(Number(v || 0)));
  }

  private syncTenderedToTotal(): void {
    if (this.method() === 'CASH') {
      const t = this.total();
      this.amountTendered.setValue(t, { emitEvent: true });
      this.tendered.set(t);
    }
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

  lookupBarcode(): void {
    const code = this.barcode.value.trim();
    this.error.set(null);
    if (!code) {
      return;
    }
    this.catalogApi.getProductByBarcode(code).subscribe({
      next: (product) => {
        this.addProduct(product);
        this.barcode.setValue('');
      },
      error: () => this.error.set('No product found for that barcode'),
    });
  }

  addProduct(product: Product): void {
    this.error.set(null);
    if (!product.active) {
      this.error.set('Product is inactive');
      return;
    }
    if (product.currentStock <= 0) {
      this.error.set(`No stock for ${product.sku}`);
      return;
    }
    const existing = this.cart().find((l) => l.productId === product.id);
    if (existing) {
      if (existing.quantity + 1 > product.currentStock) {
        this.error.set(`Insufficient stock for ${product.sku}`);
        return;
      }
      this.cart.update((rows) =>
        rows.map((l) => (l.productId === product.id ? { ...l, quantity: l.quantity + 1 } : l)),
      );
      this.searchHits.set([]);
      this.productSearch.setValue('');
      this.syncTenderedToTotal();
      return;
    }

    const customerId = this.customerId.value ? Number(this.customerId.value) : null;
    this.pricingApi.resolve(product.id, customerId).subscribe({
      next: (resolved) => {
        this.cart.update((rows) => [
          ...rows,
          {
            productId: product.id,
            sku: product.sku,
            name: product.name,
            unit: product.unit,
            quantity: 1,
            unitPrice: resolved.unitPrice,
            lineDiscount: 0,
            stock: product.currentStock,
          },
        ]);
        this.searchHits.set([]);
        this.productSearch.setValue('');
        this.syncTenderedToTotal();
      },
      error: () => {
        this.cart.update((rows) => [
          ...rows,
          {
            productId: product.id,
            sku: product.sku,
            name: product.name,
            unit: product.unit,
            quantity: 1,
            unitPrice: product.sellingPrice,
            lineDiscount: 0,
            stock: product.currentStock,
          },
        ]);
        this.searchHits.set([]);
        this.productSearch.setValue('');
        this.syncTenderedToTotal();
      },
    });
  }

  setQty(productId: number, quantity: number): void {
    const qty = Number(quantity);
    this.cart.update((rows) =>
      rows
        .map((l) => {
          if (l.productId !== productId) {
            return l;
          }
          if (qty > l.stock) {
            this.error.set(`Max stock for ${l.sku} is ${l.stock}`);
            return l;
          }
          return { ...l, quantity: Math.max(0.001, qty) };
        })
        .filter((l) => l.quantity > 0),
    );
    this.syncTenderedToTotal();
  }

  setLineDiscount(productId: number, discount: number): void {
    this.cart.update((rows) =>
      rows.map((l) => (l.productId === productId ? { ...l, lineDiscount: Math.max(0, Number(discount) || 0) } : l)),
    );
    this.syncTenderedToTotal();
  }

  setUnitPrice(productId: number, unitPrice: number): void {
    const price = Math.max(0, Number(unitPrice) || 0);
    this.cart.update((rows) =>
      rows.map((l) => (l.productId === productId ? { ...l, unitPrice: price } : l)),
    );
    this.syncTenderedToTotal();
  }

  removeLine(productId: number): void {
    this.cart.update((rows) => rows.filter((l) => l.productId !== productId));
    this.syncTenderedToTotal();
  }

  checkout(): void {
    if (this.cart().length === 0) {
      this.error.set('Add at least one product');
      return;
    }
    const method = this.paymentMethod.value;
    if (method === 'CREDIT' && !this.customerId.value) {
      this.error.set('Select a customer for credit sales');
      return;
    }
    if (method === 'CASH' && this.tendered() < this.total()) {
      this.error.set('Amount tendered is less than total');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.salesApi
      .checkout({
        customerId: this.customerId.value ? Number(this.customerId.value) : null,
        paymentMethod: method,
        discountAmount: this.discount(),
        taxAmount: this.tax(),
        amountTendered: method === 'CASH' ? this.tendered() : null,
        notes: this.notes.value || null,
        cashierName: this.cashierName.value.trim() || null,
        receivedBy: this.receivedBy.value.trim() || null,
        items: this.cart().map((l) => ({
          productId: l.productId,
          quantity: l.quantity,
          unitPrice: l.unitPrice,
          lineDiscount: l.lineDiscount,
        })),
      })
      .subscribe({
        next: (sale) => void this.router.navigate(['/sales', sale.id]),
        error: (err) => {
          this.saving.set(false);
          this.error.set(err?.error?.message || 'Checkout failed');
        },
      });
  }
}
