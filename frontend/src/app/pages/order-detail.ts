import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { OrderService, ShippingInfo } from '../services/order.service';
import { PaymentService } from '../services/payment.service';
import { Order } from '../models/models';

interface ShippingForm {
  shippingName: string;
  shippingAddress: string;
  shippingCity: string;
  shippingZip: string;
  shippingPhone: string;
}

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink],
  styleUrl: './order-detail.css',
  templateUrl: './order-detail.html',
})
export class OrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly orders = inject(OrderService);
  private readonly payments = inject(PaymentService);

  protected readonly order = signal<Order | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly paid = signal(false);
  protected readonly confirming = signal(false);
  protected readonly editing = signal(false);
  protected readonly saving = signal(false);
  protected readonly deleting = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly form = signal<ShippingForm>({
    shippingName: '',
    shippingAddress: '',
    shippingCity: '',
    shippingZip: '',
    shippingPhone: '',
  });
  private readonly id: number;

  constructor() {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    if (!Number.isFinite(this.id) || this.id <= 0) {
      this.loading.set(false);
      this.error.set(true);
      return;
    }
    this.loading.set(true);
    this.error.set(false);
    this.orders.getOrderById(this.id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        this.payments.getResultByOrderId(order.id).subscribe({
          next: (res) => this.paid.set(res.status === 'SUCCESS'),
          error: () => this.paid.set(false),
        });
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  itemsCount(order: Order): number {
    return order.items.reduce((acc: number, i) => acc + i.quantity, 0);
  }

  subtotal(order: Order): number {
    return order.items.reduce((acc: number, i) => acc + i.price * i.quantity, 0);
  }

  date(order: Order): string {
    const d = new Date(order.createdAt);
    return isNaN(d.getTime())
      ? order.createdAt
      : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) +
          ' · ' +
          d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }

  protected readonly statusSteps = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'] as const;

  statusIndex(order: Order): number {
    const idx = this.statusSteps.indexOf(order.status.toUpperCase() as (typeof this.statusSteps)[number]);
    return idx === -1 ? 0 : idx;
  }

  isCancelled(order: Order): boolean {
    return order.status.toUpperCase() === 'CANCELLED';
  }

  canPay(order: Order): boolean {
    return order.status.toUpperCase() === 'PENDING';
  }

  canConfirm(order: Order): boolean {
    return order.status.toUpperCase() === 'PENDING' && this.paid();
  }

  confirmThisOrder(): void {
    const order = this.order();
    if (!order) return;
    this.confirming.set(true);
    this.message.set(null);
    this.orders.confirmOrder(order.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.confirming.set(false);
        this.message.set('Order confirmed successfully.');
      },
      error: () => {
        this.confirming.set(false);
        this.message.set('Could not confirm order.');
      },
    });
  }

  goToPayment(order: Order): void {
    this.router.navigate(['/payment', order.id]);
  }

  startEdit(order: Order): void {
    this.form.set({
      shippingName: order.shippingName ?? '',
      shippingAddress: order.shippingAddress ?? '',
      shippingCity: order.shippingCity ?? '',
      shippingZip: order.shippingZip ?? '',
      shippingPhone: order.shippingPhone ?? '',
    });
    this.message.set(null);
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.message.set(null);
  }

  update(field: keyof ShippingForm, value: string): void {
    this.form.update((f) => ({ ...f, [field]: value }));
  }

  saveShipping(): void {
    const order = this.order();
    const userId = this.auth.userId();
    if (!order || userId === null) return;
    this.saving.set(true);
    this.message.set(null);
    const shipping: ShippingInfo = { ...this.form() };
    this.orders.updateOrder(order.id, userId, shipping).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.saving.set(false);
        this.editing.set(false);
        this.message.set('Order updated successfully.');
      },
      error: () => {
        this.saving.set(false);
        this.message.set('Could not update order.');
      },
    });
  }

  deleteThisOrder(): void {
    const order = this.order();
    const userId = this.auth.userId();
    if (!order || userId === null) return;
    if (!confirm('Delete this PENDING order? This cannot be undone.')) return;
    this.deleting.set(true);
    this.orders.deleteOrder(order.id, userId).subscribe({
      next: () => {
        this.router.navigate(['/orders']);
      },
      error: () => {
        this.deleting.set(false);
        this.message.set('Could not delete order.');
      },
    });
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