import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { InventoryApiService } from '../../../core/services/inventory-api.service';
import { Product } from '../../../core/models/catalog.models';
import { InventoryMovementLine, InventoryTransactionType } from '../../../core/models/inventory.models';

@Component({
  selector: 'app-inventory-movement',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './inventory-movement.html',
  styleUrl: './inventory-movement.scss',
})
export class InventoryMovementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly catalogApi = inject(CatalogApiService);

  readonly lines = signal<InventoryMovementLine[]>([]);
  readonly searchHits = signal<Product[]>([]);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly types: InventoryTransactionType[] = ['STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT'];

  readonly productSearch = new FormControl('', { nonNullable: true });
  readonly barcode = new FormControl('', { nonNullable: true });

  readonly form = this.fb.group({
    transactionType: this.fb.nonNullable.control<InventoryTransactionType>('STOCK_IN', Validators.required),
    referenceNo: this.fb.nonNullable.control(''),
    notes: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    const type = this.route.snapshot.queryParamMap.get('type') as InventoryTransactionType | null;
    if (type && this.types.includes(type)) {
      this.form.patchValue({ transactionType: type });
    }

    const productId = Number(this.route.snapshot.queryParamMap.get('productId') || 0);
    if (productId) {
      this.catalogApi.getProduct(productId).subscribe({
        next: (product) => this.addProduct(product),
        error: () => this.error.set('Unable to prefill product from link'),
      });
    }
  }

  quantityHint(): string {
    const type = this.form.controls.transactionType.value;
    if (type === 'ADJUSTMENT') {
      return 'For each line, enter the new absolute stock level';
    }
    if (type === 'STOCK_OUT') {
      return 'Search and add products, then enter quantities to remove';
    }
    return 'Search and add products, then enter quantities to add';
  }

  searchProducts(): void {
    const q = this.productSearch.value.trim();
    if (!q) {
      this.searchHits.set([]);
      return;
    }
    this.catalogApi.listProducts(q, null, null, 0, 12).subscribe({
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
    if (this.lines().some((l) => l.productId === product.id)) {
      this.error.set(`${product.sku} is already on the list`);
      return;
    }

    const type = this.form.controls.transactionType.value;
    const defaultQty = type === 'ADJUSTMENT' ? product.currentStock : 1;

    this.lines.update((rows) => [
      ...rows,
      {
        productId: product.id,
        sku: product.sku,
        name: product.name,
        unit: product.unit,
        stock: product.currentStock,
        quantity: defaultQty,
        unitCost: product.costPrice ?? null,
      },
    ]);
    this.searchHits.set([]);
    this.productSearch.setValue('');
  }

  setQty(productId: number, quantity: number): void {
    const qty = Number(quantity);
    if (Number.isNaN(qty)) {
      return;
    }
    this.lines.update((rows) =>
      rows.map((l) => (l.productId === productId ? { ...l, quantity: qty } : l)),
    );
  }

  setUnitCost(productId: number, unitCost: number | string): void {
    const raw = unitCost === '' || unitCost === null || unitCost === undefined ? null : Number(unitCost);
    this.lines.update((rows) =>
      rows.map((l) =>
        l.productId === productId
          ? { ...l, unitCost: raw === null || Number.isNaN(raw) ? null : raw }
          : l,
      ),
    );
  }

  removeLine(productId: number): void {
    this.lines.update((rows) => rows.filter((l) => l.productId !== productId));
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rows = this.lines();
    if (!rows.length) {
      this.error.set('Add at least one product');
      return;
    }

    const type = this.form.controls.transactionType.value;
    for (const line of rows) {
      if (type !== 'ADJUSTMENT' && line.quantity <= 0) {
        this.error.set(`Quantity for ${line.sku} must be greater than zero`);
        return;
      }
      if (type === 'ADJUSTMENT' && line.quantity < 0) {
        this.error.set(`Adjusted stock for ${line.sku} cannot be negative`);
        return;
      }
      if (type === 'STOCK_OUT' && line.quantity > line.stock) {
        this.error.set(`Insufficient stock for ${line.sku}. Available: ${line.stock}`);
        return;
      }
    }

    this.error.set(null);
    this.saving.set(true);
    const meta = this.form.getRawValue();

    this.inventoryApi
      .createBatch({
        transactionType: type,
        referenceNo: meta.referenceNo || null,
        notes: meta.notes || null,
        lines: rows.map((l) => ({
          productId: l.productId,
          quantity: Number(l.quantity),
          unitCost: l.unitCost,
        })),
      })
      .subscribe({
        next: () => void this.router.navigate(['/inventory']),
        error: (err) => {
          this.saving.set(false);
          const msg = err?.error?.message || err?.error?.detail || 'Unable to record movements';
          this.error.set(msg);
        },
      });
  }
}
