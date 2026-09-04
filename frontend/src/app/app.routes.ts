import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./pages/home').then(m => m.HomeComponent) },
  { path: 'products', loadComponent: () => import('./pages/product-list').then(m => m.ProductListComponent) },
  { path: 'products/:id', loadComponent: () => import('./pages/product-detail').then(m => m.ProductDetailComponent) },
  { path: 'login', loadComponent: () => import('./pages/login').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./pages/register').then(m => m.RegisterComponent) },
  { path: 'contact', loadComponent: () => import('./pages/contact').then(m => m.ContactComponent) },
  { path: 'policy', loadComponent: () => import('./pages/policy').then(m => m.PolicyComponent) },
  { path: 'cart', canActivate: [authGuard], loadComponent: () => import('./pages/cart').then(m => m.CartComponent) },
  { path: 'checkout', canActivate: [authGuard], loadComponent: () => import('./pages/checkout').then(m => m.CheckoutComponent) },
  { path: 'payment/:orderId', canActivate: [authGuard], loadComponent: () => 
import('./pages/payment').then(m => m.PaymentComponent) },
  { path: 'confirmation/:orderId', canActivate: [authGuard], loadComponent: () => import('./pages/order-confirmation').then(m => m.OrderConfirmationComponent) },
  { path: 'orders', canActivate: [authGuard], loadComponent: () => import('./pages/order-history').then(m => m.OrderHistoryComponent) },
  { path: 'orders/:id', canActivate: [authGuard], loadComponent: () => import('./pages/order-detail').then(m => m.OrderDetailComponent) },
  { path: 'notifications', canActivate: [authGuard], loadComponent: () => import('./pages/notifications-page').then(m => m.NotificationsPageComponent) },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./admin/admin').then(m => m.AdminComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./admin/dashboard').then(m => m.DashboardComponent) },
      { path: 'products', loadComponent: () => import('./admin/product-management').then(m => m.ProductManagementComponent) },
      { path: 'categories', loadComponent: () => import('./admin/category-management').then(m => m.CategoryManagementComponent) },
      { path: 'orders', loadComponent: () => import('./admin/order-management').then(m => m.OrderManagementComponent) },
      { path: 'users', loadComponent: () => import('./admin/user-management').then(m => m.UserManagementComponent) },
      { path: 'stock', loadComponent: () => import('./admin/stock-management').then(m => m.StockManagementComponent) },
      { path: 'analytics', loadComponent: () => import('./admin/statistics').then(m => m.StatisticsComponent) },
    ]
  },
  { path: '**', redirectTo: '/' }
];