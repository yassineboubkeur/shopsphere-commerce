import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  styleUrl: './admin.css',
  template: `
    <div class="admin-shell">
      <aside class="sidebar" [class.open]="menuOpen()">
        <a class="brand" routerLink="/admin/dashboard" (click)="close()">
          <span class="brand-mark">S</span>
          <span class="brand-title">ShopSphere <em>Admin</em></span>
        </a>
        <button type="button" class="menu-toggle" aria-label="Toggle menu" (click)="toggle()">
          <span class="bar"></span>
          <span class="bar"></span>
          <span class="bar"></span>
        </button>
        <nav class="links" (click)="close()">
          <a routerLink="/admin/dashboard" routerLinkActive="active"><span class="glyph">&#9635;</span>Dashboard</a>
          <a routerLink="/admin/products" routerLinkActive="active"><span class="glyph">&#9776;</span>Products</a>
          <a routerLink="/admin/categories" routerLinkActive="active"><span class="glyph">&#9650;</span>Categories</a>
          <a routerLink="/admin/orders" routerLinkActive="active"><span class="glyph">&#8764;</span>Orders</a>
          <a routerLink="/admin/users" routerLinkActive="active"><span class="glyph">&#9737;</span>Users</a>
          <a routerLink="/admin/stock" routerLinkActive="active"><span class="glyph">&#9668;</span>Stock</a>
          <a routerLink="/admin/analytics" routerLinkActive="active"><span class="glyph">&#9651;</span>Analytics</a>
        </nav>
        <div class="sidebar-footer">
          <button type="button" class="theme-toggle" (click)="theme.toggle()">
            @if (theme.resolved() === 'dark') {
              <span class="glyph">&#9728;</span> Light mode
            } @else {
              <span class="glyph">&#9790;</span> Dark mode
            }
          </button>
          <a class="back" routerLink="/products" (click)="close()">&larr; Back to store</a>
        </div>
      </aside>
      <main class="content" (click)="close()">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AdminComponent {
  protected readonly menuOpen = signal(false);
  protected readonly theme = inject(ThemeService);

  toggle(): void {
    this.menuOpen.update((v) => !v);
  }

  close(): void {
    this.menuOpen.set(false);
  }
}
