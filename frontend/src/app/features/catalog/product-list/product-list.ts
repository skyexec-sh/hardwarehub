import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { Brand, Category, Product } from '../../../core/models/catalog.models';

@Component({
  selector: 'app-product-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductListComponent implements OnInit {
  private readonly api = inject(CatalogApiService);

  readonly items = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly brands = signal<Brand[]>([]);
  readonly barcodeHit = signal<Product | null>(null);
  readonly barcodeError = signal<string | null>(null);

  readonly sku = new FormControl('', { nonNullable: true });
  readonly name = new FormControl('', { nonNullable: true });
  readonly categoryId = new FormControl('', { nonNullable: true });
  readonly brandId = new FormControl('', { nonNullable: true });
  readonly stockFilter = new FormControl('', { nonNullable: true });
  readonly barcode = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.api.activeCategories().subscribe({ next: (rows) => this.categories.set(rows) });
    this.api.activeBrands().subscribe({ next: (rows) => this.brands.set(rows) });
    this.load();
    merge(
      this.sku.valueChanges,
      this.name.valueChanges,
      this.categoryId.valueChanges,
      this.brandId.valueChanges,
      this.stockFilter.valueChanges,
    )
      .pipe(debounceTime(300))
      .subscribe(() => this.load());
  }

  clearFilters(): void {
    this.sku.setValue('');
    this.name.setValue('');
    this.categoryId.setValue('');
    this.brandId.setValue('');
    this.stockFilter.setValue('');
  }

  load(): void {
    this.api
      .listProducts({
        sku: this.sku.value,
        name: this.name.value,
        categoryId: this.categoryId.value ? Number(this.categoryId.value) : null,
        brandId: this.brandId.value ? Number(this.brandId.value) : null,
        lowStockOnly: this.stockFilter.value === 'low' ? true : null,
        active: this.stockFilter.value === 'inactive' ? false : this.stockFilter.value === 'active' ? true : null,
      })
      .subscribe({ next: (page) => this.items.set(page.content) });
  }

  lookupBarcode(): void {
    const code = this.barcode.value.trim();
    this.barcodeHit.set(null);
    this.barcodeError.set(null);
    if (!code) {
      return;
    }
    this.api.getProductByBarcode(code).subscribe({
      next: (product) => this.barcodeHit.set(product),
      error: () => this.barcodeError.set('No product found for that barcode'),
    });
  }

  remove(item: Product): void {
    if (!confirm(`Delete product "${item.name}"?`)) {
      return;
    }
    this.api.deleteProduct(item.id).subscribe({ next: () => this.load() });
  }
}
