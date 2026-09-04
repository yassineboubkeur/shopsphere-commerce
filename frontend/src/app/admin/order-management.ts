import { Component, computed, inject, signal } from '@angular/core';
import { OrderService } from '../services/order.service';
import { Order, OrderItem } from '../models/models';
import { ConfirmDialogService } from '../services/confirm-dialog.service';

interface StatusFlow {
  values: string[];
  next: (s: string) => string | null;
}

@Component({
  selector: 'app-order-management',
  styleUrl: './order-management.css',
  templateUrl: './order-management.html',
})
export class OrderManagementComponent {
  private readonly ordersService = inject(OrderService);
  private readonly confirm = inject(ConfirmDialogService);

  protected readonly list = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly savingId = signal<number | null>(null);
  protected readonly toast = signal('');
  protected readonly filter = signal('ALL');
  protected readonly expanded = signal<number | null>(null);

  protected readonly flow: StatusFlow = {
    values: ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'],
    next: (s: string): string | null => {
      const order = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
      const i = order.indexOf(s.toUpperCase());
      return i >= 0 && i < order.length - 1 ? order[i + 1] : null;
    },
  };

  protected readonly visible = computed(() => {
    const f = this.filter();
    if (f === 'ALL') return this.list();
    if (f === 'CANCELLED') return this.list().filter((o) => o.status.toUpperCase() === 'CANCELLED');
    return this.list().filter((o) => o.status.toUpperCase() === f);
  });

  protected readonly counts = computed(() => {
    const m = new Map<string, number>();
    for (const o of this.list()) {
      const s = o.status.toUpperCase();
      m.set(s, (m.get(s) ?? 0) + 1);
    }
    return m;
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.ordersService.getAllOrders().subscribe({
      next: (o) => {
        this.list.set([...o].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  toggle(id: number): void {
    this.expanded.set(this.expanded() === id ? null : id);
  }

  advance(o: Order): void {
    const next = this.flow.next(o.status);
    if (!next) return;
    this.savingId.set(o.id);
    this.toast.set('');
    this.ordersService.updateStatus(o.id, next).subscribe({
      next: () => {
        this.savingId.set(null);
        this.toast.set(`Order ${o.orderNumber} → ${next}.`);
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.savingId.set(null);
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  async cancel(o: Order): Promise<void> {
    if (this.flow.next(o.status) === null && o.status.toUpperCase() !== 'CANCELLED') {
      this.toast.set('Cannot cancel a delivered order.');
      setTimeout(() => this.toast.set(''), 4000);
      return;
    }
    const ok = await this.confirm.confirm({
      title: 'Cancel this order?',
      message: `Cancel order ${o.orderNumber}? This action cannot be reversed.`,
      confirmLabel: 'Cancel Order',
    });
    if (!ok) return;
    this.savingId.set(o.id);
    this.toast.set('');
    this.ordersService.updateStatus(o.id, 'CANCELLED').subscribe({
      next: () => {
        this.savingId.set(null);
        this.toast.set(`Order ${o.orderNumber} cancelled.`);
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.savingId.set(null);
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  itemsCount(order: Order): number {
    return order.items.reduce((acc: number, i: OrderItem) => acc + i.quantity, 0);
  }

  date(order: Order): string {
    const d = new Date(order.createdAt);
    return isNaN(d.getTime()) ? order.createdAt : d.toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
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

  setFilter(f: string): void {
    this.filter.set(f);
  }

  httpError(err: unknown): string {
    const anyErr = err as { error?: { message?: string; error?: string } };
    return anyErr?.error?.message || anyErr?.error?.error || 'Request failed.';
  }
}