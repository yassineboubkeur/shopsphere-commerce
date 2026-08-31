import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../services/cart.service';
import { CartItem } from '../models/models';

@Component({
  selector: 'app-cart',
  imports: [RouterLink],
  styleUrl: './cart.css',
  templateUrl: './cart.html',
})
export class CartComponent {
  private readonly cart = inject(CartService);
  protected readonly items = this.cart.items;
  protected readonly count = this.cart.count;
  protected readonly subtotal = this.cart.total;
  protected readonly shipping = computed(() => (this.cart.total() >= 100 ? 0 : 10));
  protected readonly total = computed(() => this.cart.total() + this.shipping());

  decrease(item: CartItem): void {
    this.cart.setQuantity(item.product.id, item.quantity - 1);
  }

  increase(item: CartItem): void {
    this.cart.setQuantity(item.product.id, item.quantity + 1);
  }

  onQuantity(item: CartItem, value: string): void {
    this.cart.setQuantity(item.product.id, Number(value));
  }

  remove(item: CartItem): void {
    this.cart.remove(item.product.id);
  }

  clearAll(): void {
    this.cart.clear();
  }

  money(value: number): string {
    return value.toFixed(2);
  }
}