import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { InventoryApiService } from '../../../core/services/inventory-api.service';
import { Product } from '../../../core/models/catalog.models';
import { InventoryTransactionType } from '../../../core/models/inventory.models';

@Component({
  selector: 'app-inventory-movement',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './inventory-movement.html',
  styleUrl: '../../users/user-form/user-form.scss',
})
export class InventoryMovementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly catalogApi = inject(CatalogApiService);

  readonly products = signal<Product[]>([]);
  readonly selected = signal<Product | null>(null);
  readonly error = signal<string | null>(null);
  readonly types: InventoryTransactionType[] = ['STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT'];

  readonly form = this.fb.group({
    productId: this.fb.nonNullable.control(0, [Validators.required, Validators.min(1)]),
    transactionType: this.fb.nonNullable.control<InventoryTransactionType>('STOCK_IN', Validators.required),
    quantity: this.fb.nonNullable.control(1, [Validators.required, Validators.min(0)]),
    unitCost: this.fb.control<number | null>(null),
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
      this.form.patchValue({ productId });
    }

    this.catalogApi.listProducts('', null, null, 0, 200).subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.syncSelected();
      },
    });

    this.form.controls.productId.valueChanges.subscribe(() => this.syncSelected());
    this.form.controls.transactionType.valueChanges.subscribe((t) => {
      if (t === 'ADJUSTMENT' && this.selected()) {
        this.form.patchValue({ quantity: this.selected()!.currentStock }, { emitEvent: false });
      }
    });
  }

  quantityHint(): string {
    const type = this.form.controls.transactionType.value;
    if (type === 'ADJUSTMENT') {
      return 'Enter the new absolute stock level';
    }
    if (type === 'STOCK_OUT') {
      return 'Quantity to remove from stock';
    }
    return 'Quantity to add to stock';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    const value = this.form.getRawValue();
    const type = value.transactionType;
    if (type !== 'ADJUSTMENT' && Number(value.quantity) <= 0) {
      this.error.set('Quantity must be greater than zero');
      return;
    }

    this.inventoryApi
      .create({
        productId: Number(value.productId),
        transactionType: type,
        quantity: Number(value.quantity),
        unitCost:
          value.unitCost === null || Number.isNaN(Number(value.unitCost)) ? null : Number(value.unitCost),
        referenceNo: value.referenceNo || null,
        notes: value.notes || null,
      })
      .subscribe({
        next: () => void this.router.navigate(['/inventory']),
        error: (err) => {
          const msg = err?.error?.message || err?.error?.detail || 'Unable to record movement';
          this.error.set(msg);
        },
      });
  }

  private syncSelected(): void {
    const id = Number(this.form.controls.productId.value);
    const product = this.products().find((p) => p.id === id) ?? null;
    this.selected.set(product);
    if (product && this.form.controls.transactionType.value === 'ADJUSTMENT') {
      this.form.patchValue({ quantity: product.currentStock }, { emitEvent: false });
    }
  }
}
