import { Injectable, computed, signal } from '@angular/core';
import { CartItem, Product } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSignal = signal<CartItem[]>(this.read());
  readonly items = this.itemsSignal.asReadonly();
  readonly count = computed(() => this.itemsSignal().reduce((acc, i) => acc + i.quantity, 0));
  readonly total = computed(() =>
    this.itemsSignal().reduce((acc, i) => acc + i.product.price * i.quantity, 0),
  );

  add(product: Product, quantity = 1): void {
    const items = this.itemsSignal();
    const existing = items.find((i) => i.product.id === product.id);
    let next: CartItem[];
    if (existing) {
      next = items.map((i) =>
        i.product.id === product.id ? { ...i, quantity: Math.min(i.quantity + quantity, product.stockQuantity) } : i,
      );
    } else {
      next = [...items, { product, quantity: Math.min(quantity, product.stockQuantity) }];
    }
    this.itemsSignal.set(next);
    this.persist();
  }

  setQuantity(productId: number, quantity: number): void {
    const next = this.itemsSignal().map((i) => {
      if (i.product.id !== productId) return i;
      const max = i.product.stockQuantity;
      return { ...i, quantity: Math.max(1, Math.min(quantity, max)) };
    });
    this.itemsSignal.set(next);
    this.persist();
  }

  remove(productId: number): void {
    this.itemsSignal.set(this.itemsSignal().filter((i) => i.product.id !== productId));
    this.persist();
  }

  clear(): void {
    this.itemsSignal.set([]);
    this.persist();
  }

  private persist(): void {
    localStorage.setItem('cart', JSON.stringify(this.itemsSignal()));
  }

  private read(): CartItem[] {
    try {
      const raw = localStorage.getItem('cart');
      return raw ? (JSON.parse(raw) as CartItem[]) : [];
    } catch {
      return [];
    }
  }
}