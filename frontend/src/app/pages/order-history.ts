import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { OrderService } from '../services/order.service';
import { Order, OrderItem } from '../models/models';

@Component({
  selector: 'app-order-history',
  imports: [RouterLink],
  styleUrl: './order-history.css',
  templateUrl: './order-history.html',
})
export class OrderHistoryComponent {
  private readonly auth = inject(AuthService);
  private readonly orders = inject(OrderService);

  protected readonly list = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  protected readonly sorted = computed(() =>
    [...this.list()].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
  );

  constructor() {
    this.load();
  }

  load(): void {
    const userId = this.auth.userId();
    if (userId === null) return;
    this.loading.set(true);
    this.error.set(false);
    this.orders.getOrdersByUserId(userId).subscribe({
      next: (orders) => {
        this.list.set(orders);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  itemsCount(order: Order): number {
    return order.items.reduce((acc: number, i: OrderItem) => acc + i.quantity, 0);
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