import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { CatalogApiService } from '../../../core/services/catalog-api.service';
import { Category } from '../../../core/models/catalog.models';

@Component({
  selector: 'app-category-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './category-list.html',
  styleUrl: '../../users/user-list/user-list.scss',
})
export class CategoryListComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  readonly items = signal<Category[]>([]);
  readonly search = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.load();
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load());
  }

  load(): void {
    this.api.listCategories(this.search.value).subscribe({
      next: (page) => this.items.set(page.content),
    });
  }

  remove(item: Category): void {
    if (!confirm(`Delete category "${item.name}"?`)) {
      return;
    }
    this.api.deleteCategory(item.id).subscribe({ next: () => this.load() });
  }
}
