import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { timeout, timer } from 'rxjs';
import { OrderService } from '../services/order.service';
import { ProductService } from '../services/product.service';
import { AdminService, AdminUserRow } from '../services/admin.service';
import { Order, Product, ProductCategory } from '../models/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class DashboardComponent {
  private readonly productsService = inject(ProductService);
  private readonly ordersService = inject(OrderService);
  private readonly admin = inject(AdminService);

  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<ProductCategory[]>([]);
  protected readonly orders = signal<Order[]>([]);
  protected readonly users = signal<AdminUserRow[]>([]);
  protected readonly loading = signal(true);
  protected readonly ordersError = signal(false);

  protected readonly productsCount = computed(() => this.products().length);
  protected readonly categoriesCount = computed(() => this.categories().length);
  protected readonly ordersCount = computed(() => this.orders().length);
  protected readonly usersCount = computed(() => this.users().length);
  protected readonly pendingCount = computed(() => this.orders().filter((o) => o.status.toUpperCase() === 'PENDING').length);
  protected readonly revenue = computed(() => this.orders().reduce((acc, o) => acc + o.totalAmount, 0));
  protected readonly recent = computed(() =>
    [...this.orders()]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 6),
  );

  constructor() {
    this.load();
    timer(3000).subscribe(() => {
      this.loading.set(false);
    });
  }

  load(): void {
    this.loading.set(true);
    this.ordersError.set(false);

    this.productsService.findAll().pipe(timeout(10000)).subscribe({
      next: (p) => {
        this.products.set(p);
        this.settle();
      },
      error: () => this.settle(),
    });
    this.productsService.getCategories().pipe(timeout(10000)).subscribe({
      next: (c) => {
        this.categories.set(c);
        this.settle();
      },
      error: () => this.settle(),
    });
    this.admin.getUsers().pipe(timeout(10000)).subscribe({
      next: (u) => {
        this.users.set(u);
        this.settle();
      },
      error: () => this.settle(),
    });
    this.ordersService.getAllOrders().pipe(timeout(20000)).subscribe({
      next: (o) => {
        this.orders.set(o);
        this.ordersError.set(false);
        this.settle();
      },
      error: () => {
        this.ordersError.set(true);
        this.settle();
      },
    });
  }

  private settled = 0;

  private settle(): void {
    if (++this.settled === 4) {
      this.loading.set(false);
    }
  }

  itemsCount(order: Order): number {
    return order.items.reduce((acc: number, i) => acc + i.quantity, 0);
  }

  date(order: Order): string {
    const d = new Date(order.createdAt);
    return isNaN(d.getTime()) ? order.createdAt : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  }

  money(value: number): string {
    return value.toFixed(2);
  }

  statusClass(status: string): string {
    const s = status.toUpperCase();
    if (s === 'CANCELLED') return 'cancelled';
    if (s === 'DELIVERED') return 'delivered';
    if (s === 'SHIPPED') return 'shipped';
    if (s === 'PROCESSING') return 'processing';
    if (s === 'CONFIRMED') return 'confirmed';
    return 'pending';
  }
}