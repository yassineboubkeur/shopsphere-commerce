import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';
import { Notification } from '../models/models';

@Component({
  selector: 'app-notifications-page',
  styleUrl: './notifications-page.css',
  templateUrl: './notifications-page.html',
})
export class NotificationsPageComponent {
  private readonly auth = inject(AuthService);
  private readonly notifService = inject(NotificationService);

  protected readonly list = signal<Notification[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  protected readonly sorted = computed(() =>
    [...this.list()].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')),
  );

  constructor() {
    this.load();
  }

  load(): void {
    const userId = this.auth.userId();
    if (userId === null) return;
    this.loading.set(true);
    this.error.set(false);
    this.notifService.getByUser(userId).subscribe({
      next: (list) => {
        this.list.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  isRead(n: Notification): boolean {
    return ['SEEN', 'READ', 'PROCESSED'].includes((n.status || '').toUpperCase());
  }

  timeLabel(value: string | undefined): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const now = Date.now();
    const diff = now - date.getTime();
    const minutes = Math.floor(diff / 60_000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days} d ago`;
    return date.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  }

  typeLabel(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'ORDER_CONFIRMATION': return 'Order';
      case 'PAYMENT_CONFIRMATION': return 'Payment';
      case 'SHIPPING_NOTIFICATION': return 'Shipping';
      case 'ORDER_CANCELLED': return 'Cancelled';
      case 'ORDER_DELIVERED': return 'Delivered';
      case 'PAYMENT_FAILED': return 'Payment Failed';
      case 'INVENTORY_LOW': return 'Inventory';
      default: return type;
    }
  }

  typePillClass(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'ORDER_CONFIRMATION': return 'order';
      case 'PAYMENT_CONFIRMATION': return 'payment';
      case 'SHIPPING_NOTIFICATION': return 'shipping';
      case 'ORDER_CANCELLED': return 'cancelled';
      case 'ORDER_DELIVERED': return 'delivered';
      case 'PAYMENT_FAILED': return 'failed';
      case 'INVENTORY_LOW': return 'inventory';
      default: return 'other';
    }
  }

  markAsRead(n: Notification): void {
    if (this.isRead(n)) return;
    this.notifService.markAsRead(n.id).subscribe({
      next: (updated) => {
        this.list.update((list) =>
          list.map((item) => (item.id === updated.id ? { ...item, status: updated.status } : item)),
        );
      },
      error: () => {},
    });
  }
}