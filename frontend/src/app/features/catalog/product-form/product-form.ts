import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { PricingApiService } from '../../../core/services/pricing-api.service';
import { Brand, Category } from '../../../core/models/catalog.models';
import { LevelPricePayload, PriceLevel, ProductPriceHistoryItem } from '../../../core/models/pricing.models';

@Component({
  selector: 'app-product-form',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss',
})
export class ProductFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(CatalogApiService);
  private readonly pricingApi = inject(PricingApiService);

  readonly isEdit = signal(false);
  readonly id = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly brands = signal<Brand[]>([]);
  readonly priceLevels = signal<PriceLevel[]>([]);
  readonly levelPriceControls = signal<Record<number, FormControl<number>>>({});
  readonly history = signal<ProductPriceHistoryItem[]>([]);

  readonly form = this.fb.nonNullable.group({
    sku: ['', [Validators.required, Validators.maxLength(50)]],
    barcode: [''],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    brandId: [''],
    categoryId: [''],
    unit: ['PCS', Validators.required],
    costPrice: [0, Validators.required],
    currentStock: [0, Validators.required],
    minimumStock: [0, Validators.required],
    maximumStock: [null as number | null],
    imageUrl: [''],
    active: [true],
    priceChangeReason: [''],
  });

  ngOnInit(): void {
    this.api.activeCategories().subscribe({ next: (rows) => this.categories.set(rows) });
    this.api.activeBrands().subscribe({ next: (rows) => this.brands.set(rows) });
    this.pricingApi.listLevels().subscribe({
      next: (levels) => {
        this.priceLevels.set(levels);
        const controls: Record<number, FormControl<number>> = {};
        for (const level of levels) {
          controls[level.id] = new FormControl(0, { nonNullable: true });
        }
        this.levelPriceControls.set(controls);

        const raw = this.route.snapshot.paramMap.get('id');
        if (raw && raw !== 'new') {
          const id = Number(raw);
          this.isEdit.set(true);
          this.id.set(id);
          this.api.getProduct(id).subscribe({
            next: (item) => {
              this.form.patchValue({
                sku: item.sku,
                barcode: item.barcode ?? '',
                name: item.name,
                description: item.description ?? '',
                brandId: item.brandId ? String(item.brandId) : '',
                categoryId: item.categoryId ? String(item.categoryId) : '',
                unit: item.unit,
                costPrice: item.costPrice,
                currentStock: item.currentStock,
                minimumStock: item.minimumStock,
                maximumStock: item.maximumStock ?? null,
                imageUrl: item.imageUrl ?? '',
                active: item.active,
              });
              for (const lp of item.levelPrices ?? []) {
                controls[lp.priceLevelId]?.setValue(lp.unitPrice);
              }
              const retail = levels.find((l) => l.code === 'RETAIL');
              if (retail && !(item.levelPrices ?? []).some((lp) => lp.priceLevelId === retail.id)) {
                controls[retail.id]?.setValue(item.sellingPrice);
              }
            },
          });
          this.pricingApi.priceHistory(id).subscribe({
            next: (page) => this.history.set(page.content),
          });
        }
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    const value = this.form.getRawValue();
    const levelPrices: LevelPricePayload[] = this.priceLevels().map((level) => ({
      priceLevelId: level.id,
      unitPrice: Number(this.levelPriceControls()[level.id]?.value ?? 0),
    }));
    const retail = this.priceLevels().find((l) => l.code === 'RETAIL');
    const sellingPrice = retail
      ? Number(this.levelPriceControls()[retail.id]?.value ?? 0)
      : 0;

    const payload = {
      sku: value.sku,
      barcode: value.barcode || null,
      name: value.name,
      description: value.description || null,
      brandId: value.brandId ? Number(value.brandId) : null,
      categoryId: value.categoryId ? Number(value.categoryId) : null,
      unit: value.unit,
      costPrice: Number(value.costPrice),
      sellingPrice,
      currentStock: Number(value.currentStock),
      minimumStock: Number(value.minimumStock),
      maximumStock:
        value.maximumStock == null || value.maximumStock === ('' as unknown) ? null : Number(value.maximumStock),
      imageUrl: value.imageUrl || null,
      active: value.active,
      levelPrices,
      priceChangeReason: value.priceChangeReason || null,
    };

    const req = this.isEdit()
      ? this.api.updateProduct(this.id()!, payload)
      : this.api.createProduct(payload);
    req.subscribe({
      next: () => void this.router.navigate(['/products']),
      error: () => this.error.set('Unable to save product'),
    });
  }
}
