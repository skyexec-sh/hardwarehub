import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogApiService } from '../../../core/services/catalog-api.service';

@Component({
  selector: 'app-brand-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './brand-form.html',
  styleUrl: '../../users/user-form/user-form.scss',
})
export class BrandFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(CatalogApiService);

  readonly isEdit = signal(false);
  readonly id = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
    logoUrl: [''],
    active: [true],
  });

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id');
    if (raw && raw !== 'new') {
      const id = Number(raw);
      this.isEdit.set(true);
      this.id.set(id);
      this.api.getBrand(id).subscribe({
        next: (item) =>
          this.form.patchValue({
            name: item.name,
            description: item.description ?? '',
            logoUrl: item.logoUrl ?? '',
            active: item.active,
          }),
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    const value = this.form.getRawValue();
    const payload = {
      name: value.name,
      description: value.description || null,
      logoUrl: value.logoUrl || null,
      active: value.active,
    };
    const req = this.isEdit() ? this.api.updateBrand(this.id()!, payload) : this.api.createBrand(payload);
    req.subscribe({
      next: () => void this.router.navigate(['/brands']),
      error: () => this.error.set('Unable to save brand'),
    });
  }
}
