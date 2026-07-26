import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { CustomerApiService } from '../../../core/services/customer-api.service';
import { Customer, CustomerStatus } from '../../../core/models/customer.models';

@Component({
  selector: 'app-customer-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './customer-list.html',
  styleUrl: './customer-list.scss',
})
export class CustomerListComponent implements OnInit {
  private readonly api = inject(CustomerApiService);
  private readonly route = inject(ActivatedRoute);

  readonly items = signal<Customer[]>([]);
  readonly code = new FormControl('', { nonNullable: true });
  readonly businessName = new FormControl('', { nonNullable: true });
  readonly contact = new FormControl('', { nonNullable: true });
  readonly phone = new FormControl('', { nonNullable: true });
  readonly city = new FormControl('', { nonNullable: true });
  readonly balanceDue = new FormControl('', { nonNullable: true });
  readonly status = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('balanceDue') === '1') {
      this.balanceDue.setValue('1', { emitEvent: false });
    }
    this.load();
    merge(
      this.code.valueChanges,
      this.businessName.valueChanges,
      this.contact.valueChanges,
      this.phone.valueChanges,
      this.city.valueChanges,
      this.balanceDue.valueChanges,
      this.status.valueChanges,
    )
      .pipe(debounceTime(300))
      .subscribe(() => this.load());
  }

  clearFilters(): void {
    this.code.setValue('');
    this.businessName.setValue('');
    this.contact.setValue('');
    this.phone.setValue('');
    this.city.setValue('');
    this.balanceDue.setValue('');
    this.status.setValue('');
  }

  load(): void {
    this.api
      .list({
        code: this.code.value,
        businessName: this.businessName.value,
        contact: this.contact.value,
        phone: this.phone.value,
        city: this.city.value,
        status: this.status.value as CustomerStatus | '',
        hasBalanceDue: this.balanceDue.value === '1' ? true : null,
      })
      .subscribe({ next: (page) => this.items.set(page.content) });
  }

  setStatus(item: Customer, status: CustomerStatus): void {
    this.api.updateStatus(item.id, status).subscribe({ next: () => this.load() });
  }

  remove(item: Customer): void {
    if (!confirm(`Delete customer "${item.businessName}"?`)) {
      return;
    }
    this.api.remove(item.id).subscribe({ next: () => this.load() });
  }
}
