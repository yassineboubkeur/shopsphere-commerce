import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../services/product.service';
import { Product } from '../models/models';

@Component({
  selector: 'app-stock-management',
  imports: [FormsModule],
  styleUrl: './stock-management.css',
  templateUrl: './stock-management.html',
})
export class StockManagementComponent {
  private readonly productsService = inject(ProductService);

  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly savingId = signal<number | null>(null);
  protected readonly toast = signal('');
  protected readonly filter = signal('ALL');

  protected editingId: number | null = null;
  protected editValue = '';

  protected readonly visible = computed(() => {
    const f = this.filter();
    if (f === 'ALL') return this.products();
    if (f === 'LOW') return this.products().filter((p) => p.stockQuantity <= 5);
    if (f === 'OUT') return this.products().filter((p) => p.stockQuantity === 0);
    if (f === 'IN') return this.products().filter((p) => p.stockQuantity > 5);
    return this.products();
  });

  protected readonly lowCount = computed(() => this.products().filter((p) => p.stockQuantity <= 5).length);
  protected readonly outCount = computed(() => this.products().filter((p) => p.stockQuantity === 0).length);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.productsService.findAll().subscribe({
      next: (p) => {
        this.products.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  startEdit(p: Product): void {
    this.editingId = p.id;
    this.editValue = p.stockQuantity.toString();
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editValue = '';
  }

  save(p: Product): void {
    const qty = Number(this.editValue);
    if (isNaN(qty) || qty < 0 || !Number.isInteger(qty)) {
      this.toast.set('Stock must be a whole number ≥ 0.');
      setTimeout(() => this.toast.set(''), 3500);
      return;
    }
    this.savingId.set(p.id);
    this.toast.set('');
    this.productsService.updateStock(p.id, qty).subscribe({
      next: () => {
        this.savingId.set(null);
        this.editingId = null;
        this.editValue = '';
        this.toast.set(`${p.name} stock → ${qty}.`);
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

  setFilter(f: string): void {
    this.filter.set(f);
  }

  statusFor(p: Product): { label: string; cls: string } {
    if (p.stockQuantity === 0) return { label: 'Out of stock', cls: 'out' };
    if (p.stockQuantity <= 5) return { label: 'Low stock', cls: 'low' };
    return { label: 'In stock', cls: 'ok' };
  }

  money(value: number): string {
    return value.toFixed(2);
  }

  httpError(err: unknown): string {
    const anyErr = err as { error?: { message?: string; error?: string } };
    return anyErr?.error?.message || anyErr?.error?.error || 'Request failed.';
  }
}