import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/auth/auth.service';

export interface PageCrumb {
  label: string;
  link?: string;
}

interface PageMeta {
  title: string;
  crumbs: PageCrumb[];
}

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
  readonly crumbs = signal<PageCrumb[]>([{ label: 'Main' }, { label: 'Dashboard' }]);

  constructor() {
    this.applyRoute(this.router.url);
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe((e) => {
      this.applyRoute((e as NavigationEnd).urlAfterRedirects);
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  logout(): void {
    this.auth.logout();
  }

  private applyRoute(url: string): void {
    const meta = this.resolveMeta(url.split('?')[0]);
    this.pageTitle.set(meta.title);
    this.crumbs.set(meta.crumbs);
  }

  private resolveMeta(path: string): PageMeta {
    if (path.startsWith('/users')) {
      return meta('User Management', [{ label: 'Main', link: '/dashboard' }, { label: 'Users' }]);
    }
    if (path.startsWith('/change-password')) {
      return meta('Change Password', [{ label: 'Account' }, { label: 'Change password' }]);
    }
    if (path.startsWith('/products/new')) {
      return meta('New Product', [
        { label: 'Catalog' },
        { label: 'Products', link: '/products' },
        { label: 'New' },
      ]);
    }
    if (path.match(/^\/products\/\d+/)) {
      return meta('Edit Product', [
        { label: 'Catalog' },
        { label: 'Products', link: '/products' },
        { label: 'Edit' },
      ]);
    }
    if (path.startsWith('/products')) {
      return meta('Products', [{ label: 'Catalog' }, { label: 'Products' }]);
    }
    if (path.startsWith('/categories')) {
      return meta('Categories', [{ label: 'Catalog' }, { label: 'Categories' }]);
    }
    if (path.startsWith('/brands')) {
      return meta('Brands', [{ label: 'Catalog' }, { label: 'Brands' }]);
    }
    if (path.startsWith('/price-levels')) {
      return meta('Price Levels', [{ label: 'Catalog' }, { label: 'Price levels' }]);
    }
    if (path.startsWith('/inventory/move')) {
      return meta('Record Movement', [
        { label: 'Inventory' },
        { label: 'Movements', link: '/inventory' },
        { label: 'Record' },
      ]);
    }
    if (path.startsWith('/inventory/low-stock')) {
      return meta('Low Stock Alerts', [{ label: 'Inventory' }, { label: 'Low stock' }]);
    }
    if (path.startsWith('/inventory')) {
      return meta('Inventory Movements', [{ label: 'Inventory' }, { label: 'Movements' }]);
    }
    if (path.startsWith('/customers/new')) {
      return meta('New Customer', [
        { label: 'Sales' },
        { label: 'Customers', link: '/customers' },
        { label: 'New' },
      ]);
    }
    if (path.match(/^\/customers\/\d+\/history/)) {
      return meta('Customer Account', [
        { label: 'Sales' },
        { label: 'Customers', link: '/customers' },
        { label: 'Account' },
      ]);
    }
    if (path.match(/^\/customers\/\d+/)) {
      return meta('Edit Customer', [
        { label: 'Sales' },
        { label: 'Customers', link: '/customers' },
        { label: 'Edit' },
      ]);
    }
    if (path.startsWith('/customers')) {
      return meta('Customers', [{ label: 'Sales' }, { label: 'Customers' }]);
    }
    if (path.startsWith('/sales/pos')) {
      return meta('Point of Sale', [{ label: 'Sales' }, { label: 'POS' }]);
    }
    if (path.match(/^\/sales\/\d+/)) {
      return meta('Sale Receipt', [
        { label: 'Sales' },
        { label: 'Sales', link: '/sales' },
        { label: 'Receipt' },
      ]);
    }
    if (path.startsWith('/sales')) {
      return meta('Sales', [{ label: 'Sales' }, { label: 'Sales history' }]);
    }
    if (path.startsWith('/fulfillment/quotes/new')) {
      return meta('New Quotation', [
        { label: 'Sales' },
        { label: 'Quotes', link: '/fulfillment/quotes' },
        { label: 'New' },
      ]);
    }
    if (path.match(/^\/fulfillment\/quotes\/\d+\/edit/)) {
      return meta('Edit Quotation', [
        { label: 'Sales' },
        { label: 'Quotes', link: '/fulfillment/quotes' },
        { label: 'Edit' },
      ]);
    }
    if (path.match(/^\/fulfillment\/quotes\/\d+/)) {
      return meta('Quotation', [
        { label: 'Sales' },
        { label: 'Quotes', link: '/fulfillment/quotes' },
        { label: 'Detail' },
      ]);
    }
    if (path.startsWith('/fulfillment/quotes')) {
      return meta('Quotations', [{ label: 'Sales' }, { label: 'Quotes' }]);
    }
    if (path.match(/^\/fulfillment\/orders\/\d+/)) {
      return meta('Sales Order', [
        { label: 'Sales' },
        { label: 'Orders', link: '/fulfillment/orders' },
        { label: 'Detail' },
      ]);
    }
    if (path.startsWith('/fulfillment/orders')) {
      return meta('Sales Orders', [{ label: 'Sales' }, { label: 'Orders' }]);
    }
    if (path.match(/^\/fulfillment\/deliveries\/\d+/)) {
      return meta('Delivery Receipt', [
        { label: 'Sales' },
        { label: 'Orders', link: '/fulfillment/orders' },
        { label: 'Delivery' },
      ]);
    }
    if (path.match(/^\/fulfillment\/invoices\/\d+/)) {
      return meta('Fulfillment Invoice', [
        { label: 'Sales' },
        { label: 'Invoices', link: '/fulfillment/invoices' },
        { label: 'Detail' },
      ]);
    }
    if (path.startsWith('/fulfillment/invoices')) {
      return meta('Fulfillment Invoices', [{ label: 'Sales' }, { label: 'Invoices' }]);
    }

    return meta('Dashboard', [{ label: 'Main' }, { label: 'Dashboard' }]);
  }
}

function meta(title: string, crumbs: PageCrumb[]): PageMeta {
  return { title, crumbs };
}
