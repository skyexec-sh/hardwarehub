import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CustomerApiService } from '../../../core/services/customer-api.service';
import { PricingApiService } from '../../../core/services/pricing-api.service';
import { CustomerStatus } from '../../../core/models/customer.models';
import { PriceLevel } from '../../../core/models/pricing.models';

@Component({
  selector: 'app-customer-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './customer-form.html',
  styleUrl: '../../users/user-form/user-form.scss',
})
export class CustomerFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(CustomerApiService);
  private readonly pricingApi = inject(PricingApiService);

  readonly isEdit = signal(false);
  readonly id = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly outstandingBalance = signal(0);
  readonly priceLevels = signal<PriceLevel[]>([]);
  readonly statuses: CustomerStatus[] = ['ACTIVE', 'INACTIVE', 'ON_HOLD'];

  readonly form = this.fb.nonNullable.group({
    customerCode: ['', [Validators.required, Validators.maxLength(30)]],
    businessName: ['', [Validators.required, Validators.maxLength(200)]],
    contactPerson: [''],
    phone: [''],
    email: [''],
    address: [''],
    city: [''],
    province: [''],
    taxIdentificationNumber: [''],
    notes: [''],
    creditLimit: [0, Validators.required],
    priceLevelId: [''],
    status: ['ACTIVE' as CustomerStatus, Validators.required],
  });

  ngOnInit(): void {
    this.pricingApi.listLevels(true).subscribe({
      next: (levels) => {
        this.priceLevels.set(levels);
        const retail = levels.find((l) => l.code === 'RETAIL');
        if (retail && !this.form.controls.priceLevelId.value) {
          this.form.patchValue({ priceLevelId: String(retail.id) });
        }
      },
    });

    const raw = this.route.snapshot.paramMap.get('id');
    if (raw && raw !== 'new') {
      const id = Number(raw);
      this.isEdit.set(true);
      this.id.set(id);
      this.api.get(id).subscribe({
        next: (item) => {
          this.outstandingBalance.set(item.outstandingBalance);
          this.form.patchValue({
            customerCode: item.customerCode,
            businessName: item.businessName,
            contactPerson: item.contactPerson ?? '',
            phone: item.phone ?? '',
            email: item.email ?? '',
            address: item.address ?? '',
            city: item.city ?? '',
            province: item.province ?? '',
            taxIdentificationNumber: item.taxIdentificationNumber ?? '',
            notes: item.notes ?? '',
            creditLimit: item.creditLimit,
            priceLevelId: item.priceLevelId ? String(item.priceLevelId) : '',
            status: item.status,
          });
        },
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
      customerCode: value.customerCode,
      businessName: value.businessName,
      contactPerson: value.contactPerson || null,
      phone: value.phone || null,
      email: value.email || null,
      address: value.address || null,
      city: value.city || null,
      province: value.province || null,
      taxIdentificationNumber: value.taxIdentificationNumber || null,
      notes: value.notes || null,
      creditLimit: Number(value.creditLimit),
      priceLevelId: value.priceLevelId ? Number(value.priceLevelId) : null,
      status: value.status,
    };

    const req = this.isEdit() ? this.api.update(this.id()!, payload) : this.api.create(payload);
    req.subscribe({
      next: () => void this.router.navigate(['/customers']),
      error: () => this.error.set('Unable to save customer'),
    });
  }
}
