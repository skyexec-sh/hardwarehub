import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatMenuModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.auth.user;
  readonly canManageUsers = computed(() => this.auth.hasAnyRole(['OWNER', 'ADMIN']));
  readonly canManageCatalog = computed(() =>
    this.auth.hasAnyRole(['OWNER', 'ADMIN', 'MANAGER', 'INVENTORY_STAFF']),
  );
  readonly sidebarOpen = signal(true);
  readonly pageTitle = signal('Dashboard');

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe((e) => {
      const url = (e as NavigationEnd).urlAfterRedirects;
      if (url.startsWith('/users')) {
        this.pageTitle.set('User Management');
      } else if (url.startsWith('/products')) {
        this.pageTitle.set('Products');
      } else if (url.startsWith('/categories')) {
        this.pageTitle.set('Categories');
      } else if (url.startsWith('/brands')) {
        this.pageTitle.set('Brands');
      } else if (url.startsWith('/customers')) {
        this.pageTitle.set('Customers');
      } else if (url.startsWith('/sales/pos')) {
        this.pageTitle.set('Point of Sale');
      } else if (url.match(/^\/sales\/\d+/)) {
        this.pageTitle.set('Sale Receipt');
      } else if (url.startsWith('/sales')) {
        this.pageTitle.set('Sales');
      } else if (url.startsWith('/inventory/low-stock')) {
        this.pageTitle.set('Low Stock Alerts');
      } else if (url.startsWith('/inventory/move')) {
        this.pageTitle.set('Record Movement');
      } else if (url.startsWith('/inventory')) {
        this.pageTitle.set('Inventory Movements');
      } else if (url.startsWith('/change-password')) {
        this.pageTitle.set('Change Password');
      } else {
        this.pageTitle.set('Dashboard');
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  logout(): void {
    this.auth.logout();
  }
}
