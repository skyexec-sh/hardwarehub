import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CustomerApiService } from '../../../core/services/customer-api.service';
import { Customer, CustomerPurchaseHistoryItem } from '../../../core/models/customer.models';

@Component({
  selector: 'app-customer-history',
  imports: [RouterLink, DatePipe],
  templateUrl: './customer-history.html',
  styleUrl: '../../users/user-list/user-list.scss',
})
export class CustomerHistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(CustomerApiService);

  readonly customer = signal<Customer | null>(null);
  readonly history = signal<CustomerPurchaseHistoryItem[]>([]);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.get(id).subscribe({ next: (c) => this.customer.set(c) });
    this.api.purchaseHistory(id).subscribe({ next: (rows) => this.history.set(rows) });
  }
}
