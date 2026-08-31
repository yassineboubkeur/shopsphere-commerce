import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { OrderService } from '../services/order.service';
import { PaymentService } from '../services/payment.service';
import { Order, OrderItem } from '../models/models';

interface Card {
  cardNumber: string;
  cardHolder: string;
  expiryDate: string;
  cvv: string;
}

@Component({
  selector: 'app-payment',
  imports: [RouterLink],
  styleUrl: './payment.css',
  templateUrl: './payment.html',
})
export class PaymentComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly orders = inject(OrderService);
  private readonly payments = inject(PaymentService);

  protected readonly order = signal<Order | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly card = signal<Card>({ cardNumber: '', cardHolder: '', expiryDate: '', cvv: '' });
  protected readonly errors = signal<string[]>([]);
  protected readonly paying = signal(false);
  protected readonly paymentError = signal<string | null>(null);

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
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set('Could not load your order. It may have been processed already.');
      },
    });
  }

  update(field: keyof Card, value: string): void {
    const clean =
      field === 'cardNumber'
        ? value.replace(/\D/g, '').slice(0, 16)
        : field === 'expiryDate'
          ? this.formatExpiry(value)
          : field === 'cvv'
            ? value.replace(/\D/g, '').slice(0, 4)
            : value;
    this.card.update((c) => ({ ...c, [field]: clean }));
  }

  pay(): void {
    const card = this.card();
    const order = this.order();
    const userId = this.auth.userId();
    if (!order || userId === null) return;

    const errs: string[] = [];
    if (card.cardNumber.length < 16) errs.push('Card number must be 16 digits.');
    if (!card.cardHolder.trim()) errs.push('Card holder name is required.');
    if (!/^\d{2}\/\d{2}$/.test(card.expiryDate)) errs.push('Expiry date must be MM/YY.');
    if (card.cvv.length < 3) errs.push('CVV must be at least 3 digits.');
    this.errors.set(errs);
    if (errs.length > 0) return;

    this.paying.set(true);
    this.paymentError.set(null);
    this.payments
      .process({
        orderId: order.id,
        userId,
        amount: order.totalAmount,
        paymentMethod: 'CARD',
        cardNumber: card.cardNumber,
        cardHolder: card.cardHolder,
        expiryDate: card.expiryDate,
        cvv: card.cvv,
      })
      .subscribe({
        next: (res) => {
          this.paying.set(false);
          if (res.status === 'SUCCESS') {
            this.router.navigate(['/confirmation', order.id]);
          } else {
            this.paymentError.set(res.message || 'Payment failed. Please check your card details.');
          }
        },
        error: () => {
          this.paying.set(false);
          this.paymentError.set('Payment service is unreachable. Please try again.');
        },
      });
  }

  totalItems(order: Order): number {
    return order.items.reduce((acc: number, i: OrderItem) => acc + i.quantity, 0);
  }

  money(value: number): string {
    return value.toFixed(2);
  }

  private formatExpiry(value: string): string {
    const digits = value.replace(/\D/g, '').slice(0, 4);
    if (digits.length >= 3) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
    return digits;
  }
}
