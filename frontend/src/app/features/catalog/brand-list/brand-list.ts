import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { Brand } from '../../../core/models/catalog.models';

@Component({
  selector: 'app-brand-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './brand-list.html',
  styleUrl: '../../users/user-list/user-list.scss',
})
export class BrandListComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  readonly items = signal<Brand[]>([]);
  readonly search = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load());
  }

  load(): void {
    this.api.listBrands(this.search.value).subscribe({
      next: (page) => this.items.set(page.content),
    });
  }

  remove(item: Brand): void {
    if (!confirm(`Delete brand "${item.name}"?`)) {
      return;
    }
    this.api.deleteBrand(item.id).subscribe({ next: () => this.load() });
  }
}
