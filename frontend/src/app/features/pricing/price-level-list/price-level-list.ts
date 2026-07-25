import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { PricingApiService } from '../../../core/services/pricing-api.service';
import { PriceLevel } from '../../../core/models/pricing.models';

@Component({
  selector: 'app-price-level-list',
  imports: [ReactiveFormsModule],
  templateUrl: './price-level-list.html',
  styleUrl: './price-level-list.scss',
})
export class PriceLevelListComponent implements OnInit {
  private readonly api = inject(PricingApiService);

  readonly levels = signal<PriceLevel[]>([]);
  readonly error = signal<string | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly name = new FormControl('', { nonNullable: true });
  readonly description = new FormControl('', { nonNullable: true });
  readonly active = new FormControl(true, { nonNullable: true });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.listLevels().subscribe({
      next: (rows) => this.levels.set(rows),
      error: () => this.error.set('Unable to load price levels'),
    });
  }

  startEdit(level: PriceLevel): void {
    this.editingId.set(level.id);
    this.name.setValue(level.name);
    this.description.setValue(level.description ?? '');
    this.active.setValue(level.active);
    this.error.set(null);
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  save(level: PriceLevel): void {
    this.api
      .updateLevel(level.id, {
        name: this.name.value.trim(),
        description: this.description.value.trim() || null,
        active: this.active.value,
      })
      .subscribe({
        next: () => {
          this.editingId.set(null);
          this.load();
        },
        error: (err) => this.error.set(err?.error?.message || 'Unable to save price level'),
      });
  }
}
