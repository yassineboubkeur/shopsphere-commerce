import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../services/cart.service';
import { AuthService } from '../services/auth.service';
import { OrderService, OrderItemRequest } from '../services/order.service';

interface Shipping {
  fullName: string;
  address: string;
  city: string;
  zip: string;
  phone: string;
}

@Component({
  selector: 'app-checkout',
  imports: [RouterLink],
  styleUrl: './checkout.css',
  templateUrl: './checkout.html',
})
export class CheckoutComponent {
  private readonly cart = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly orders = inject(OrderService);
  private readonly router = inject(Router);

  protected readonly items = this.cart.items;
  protected readonly count = this.cart.count;
  protected readonly subtotal = this.cart.total;
  protected readonly shipping = computed(() => (this.cart.total() >= 100 ? 0 : 10));
  protected readonly total = computed(() => this.cart.total() + this.shipping());

  protected readonly shippingForm = signal<Shipping>(this.readShipping());
  protected readonly errors = signal<string[]>([]);
  protected readonly placing = signal(false);
  protected readonly failed = signal<string | null>(null);

  update(field: keyof Shipping, value: string): void {
    this.shippingForm.update((f) => ({ ...f, [field]: value }));
  }

  placeOrder(): void {
    const form = this.shippingForm();
    const errs: string[] = [];
    if (!form.fullName.trim()) errs.push('Full name is required.');
    if (!form.address.trim()) errs.push('Address is required.');
    if (!form.city.trim()) errs.push('City is required.');
    if (!form.zip.trim()) errs.push('ZIP / postal code is required.');
    if (!form.phone.trim()) errs.push('Phone number is required.');
    this.errors.set(errs);
    if (errs.length > 0) return;

    const userId = this.auth.userId();
    if (userId === null || this.items().length === 0) return;

    const orderItems: OrderItemRequest[] = this.items().map((i) => ({
      productId: i.product.id,
      productName: i.product.name,
      price: i.product.price,
      quantity: i.quantity,
    }));

    this.placing.set(true);
    this.failed.set(null);
    this.orders.placeOrder(userId, orderItems, {
      shippingName: form.fullName,
      shippingAddress: form.address,
      shippingCity: form.city,
      shippingZip: form.zip,
      shippingPhone: form.phone,
    }).subscribe({
      next: (order) => {
        this.cart.clear();
        this.saveShipping(form);
        this.router.navigate(['/payment', order.id]);
      },
      error: () => {
        this.placing.set(false);
        this.orders.getOrdersByUserId(userId).subscribe({
          next: (recent) => {
            const match = recent.find(
              (o) =>
                o.createdAt &&
                Date.now() - new Date(o.createdAt).getTime() < 30000 &&
                Number(o.totalAmount) === this.subtotal() &&
                o.items.length === this.items().length,
            );
            if (match) {
              this.cart.clear();
              this.saveShipping(form);
              this.router.navigate(['/payment', match.id]);
            } else {
              this.failed.set('Could not place your order. Please try again.');
            }
          },
          error: () => this.failed.set('Could not place your order. Please try again.'),
        });
      },
    });
  }

  money(value: number): string {
    return value.toFixed(2);
  }

  private readShipping(): Shipping {
    try {
      const raw = localStorage.getItem('shipping');
      if (raw) return { ...{ fullName: '', address: '', city: '', zip: '', phone: '' }, ...JSON.parse(raw) };
    } catch {
      /* ignore */
    }
    return { fullName: '', address: '', city: '', zip: '', phone: '' };
  }

  private saveShipping(f: Shipping): void {
    localStorage.setItem('shipping', JSON.stringify(f));
  }
}
