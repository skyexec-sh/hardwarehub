import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guard';

const catalogRoles = ['OWNER', 'ADMIN', 'MANAGER', 'INVENTORY_STAFF'] as const;

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPasswordComponent),
  },
  {
    path: 'reset-password',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password').then((m) => m.ResetPasswordComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard').then((m) => m.DashboardComponent),
      },
      {
        path: 'change-password',
        loadComponent: () =>
          import('./features/auth/change-password/change-password').then(
            (m) => m.ChangePasswordComponent,
          ),
      },
      {
        path: 'users',
        canActivate: [roleGuard(['OWNER', 'ADMIN'])],
        loadComponent: () =>
          import('./features/users/user-list/user-list').then((m) => m.UserListComponent),
      },
      {
        path: 'users/new',
        canActivate: [roleGuard(['OWNER', 'ADMIN'])],
        loadComponent: () =>
          import('./features/users/user-form/user-form').then((m) => m.UserFormComponent),
      },
      {
        path: 'users/:id',
        canActivate: [roleGuard(['OWNER', 'ADMIN'])],
        loadComponent: () =>
          import('./features/users/user-form/user-form').then((m) => m.UserFormComponent),
      },
      {
        path: 'categories',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/category-list/category-list').then((m) => m.CategoryListComponent),
      },
      {
        path: 'categories/new',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/category-form/category-form').then((m) => m.CategoryFormComponent),
      },
      {
        path: 'categories/:id',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/category-form/category-form').then((m) => m.CategoryFormComponent),
      },
      {
        path: 'brands',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/brand-list/brand-list').then((m) => m.BrandListComponent),
      },
      {
        path: 'brands/new',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/brand-form/brand-form').then((m) => m.BrandFormComponent),
      },
      {
        path: 'brands/:id',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/brand-form/brand-form').then((m) => m.BrandFormComponent),
      },
      {
        path: 'products',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/catalog/product-list/product-list').then((m) => m.ProductListComponent),
      },
      {
        path: 'products/new',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/product-form/product-form').then((m) => m.ProductFormComponent),
      },
      {
        path: 'products/:id',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/catalog/product-form/product-form').then((m) => m.ProductFormComponent),
      },
      {
        path: 'customers',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/customers/customer-list/customer-list').then((m) => m.CustomerListComponent),
      },
      {
        path: 'customers/new',
        canActivate: [roleGuard(['OWNER', 'ADMIN', 'MANAGER', 'CASHIER'])],
        loadComponent: () =>
          import('./features/customers/customer-form/customer-form').then((m) => m.CustomerFormComponent),
      },
      {
        path: 'customers/:id/history',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/customers/customer-history/customer-history').then(
            (m) => m.CustomerHistoryComponent,
          ),
      },
      {
        path: 'customers/:id',
        canActivate: [roleGuard(['OWNER', 'ADMIN', 'MANAGER', 'CASHIER'])],
        loadComponent: () =>
          import('./features/customers/customer-form/customer-form').then((m) => m.CustomerFormComponent),
      },
      {
        path: 'inventory',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/inventory/inventory-history/inventory-history').then(
            (m) => m.InventoryHistoryComponent,
          ),
      },
      {
        path: 'inventory/move',
        canActivate: [roleGuard([...catalogRoles])],
        loadComponent: () =>
          import('./features/inventory/inventory-movement/inventory-movement').then(
            (m) => m.InventoryMovementComponent,
          ),
      },
      {
        path: 'inventory/low-stock',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/inventory/inventory-low-stock/inventory-low-stock').then(
            (m) => m.InventoryLowStockComponent,
          ),
      },
      {
        path: 'sales',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/sales/sale-list/sale-list').then((m) => m.SaleListComponent),
      },
      {
        path: 'sales/pos',
        canActivate: [roleGuard(['OWNER', 'ADMIN', 'MANAGER', 'CASHIER'])],
        loadComponent: () => import('./features/sales/pos/pos').then((m) => m.PosComponent),
      },
      {
        path: 'sales/:id',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/sales/sale-receipt/sale-receipt').then((m) => m.SaleReceiptComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
