import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../services/order.service';
import { PaymentService, PaymentResult } from '../services/payment.service';
import { Order } from '../models/models';

@Component({
  selector: 'app-order-confirmation',
  imports: [RouterLink],
  styleUrl: './order-confirmation.css',
  templateUrl: './order-confirmation.html',
})
export class OrderConfirmationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orders = inject(OrderService);
  private readonly payments = inject(PaymentService);

  protected readonly order = signal<Order | null>(null);
  protected readonly payment = signal<PaymentResult | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly confirming = signal(false);
  protected readonly confirmError = signal<string | null>(null);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('orderId'));
    if (!Number.isFinite(id) || id <= 0) {
      this.loading.set(false);
      this.loadError.set('Invalid order.');
      return;
    }
    this.orders.getOrderById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        this.payments.getResultByOrderId(order.id).subscribe({
          next: (p) => this.payment.set(p),
          error: () => this.payment.set(null),
        });
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set('Could not load your order confirmation.');
      },
    });
  }

  canConfirm(order: Order): boolean {
    return order.status.toUpperCase() === 'PENDING';
  }

  confirm(): void {
    const order = this.order();
    if (!order) return;
    this.confirming.set(true);
    this.confirmError.set(null);
    this.orders.confirmOrder(order.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.confirming.set(false);
      },
      error: () => {
        this.confirming.set(false);
        this.confirmError.set('Could not confirm order. Please try again.');
      },
    });
  }

  totalQty(order: Order): number {
    return order.items.reduce((acc: number, i) => acc + i.quantity, 0);
  }

  money(value: number): string {
    return value.toFixed(2);
  }
}