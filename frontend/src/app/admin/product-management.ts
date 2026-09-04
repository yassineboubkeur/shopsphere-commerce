import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductService, ProductRequest } from '../services/product.service';
import { Product, ProductCategory } from '../models/models';
import { ConfirmDialogService } from '../services/confirm-dialog.service';

@Component({
  selector: 'app-product-management',
  imports: [FormsModule],
  styleUrl: './product-management.css',
  templateUrl: './product-management.html',
})
export class ProductManagementComponent {
  private readonly productsService = inject(ProductService);
  private readonly confirm = inject(ConfirmDialogService);

  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<ProductCategory[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly saving = signal(false);
  protected readonly toast = signal('');
  protected editing = false;
  protected editId: number | null = null;

  protected name = '';
  protected description = '';
  protected price = '';
  protected stockQuantity = '';
  protected imageUrl = '';
  protected categoryId: number | null = null;

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
    this.productsService.getCategories().subscribe({
      next: (c) => this.categories.set(c),
    });
  }

  openNew(): void {
    this.editing = true;
    this.editId = null;
    this.name = '';
    this.description = '';
    this.price = '';
    this.stockQuantity = '';
    this.imageUrl = '';
    this.categoryId = this.categories()[0]?.id ?? null;
  }

  openEdit(p: Product): void {
    this.editing = true;
    this.editId = p.id;
    this.name = p.name;
    this.description = p.description ?? '';
    this.price = p.price.toString();
    this.stockQuantity = p.stockQuantity.toString();
    this.imageUrl = p.imageUrl ?? '';
    this.categoryId = p.category?.id ?? null;
  }

  cancel(): void {
    this.editing = false;
    this.editId = null;
  }

  save(): void {
    const price = Number(this.price);
    const stock = Number(this.stockQuantity);
    if (!this.name.trim() || isNaN(price) || price <= 0 || isNaN(stock) || stock < 0 || this.categoryId === null) {
      this.toast.set('Fill name, valid price, stock and category.');
      setTimeout(() => this.toast.set(''), 3500);
      return;
    }
    const request: ProductRequest = {
      name: this.name.trim(),
      description: this.description.trim(),
      price,
      stockQuantity: stock,
      imageUrl: this.imageUrl.trim(),
      categoryId: this.categoryId,
    };
    this.saving.set(true);
    this.toast.set('');
    const isCreate = this.editId === null;
    const call$ = isCreate
      ? this.productsService.create(request)
      : this.productsService.update(this.editId!, request);
    call$.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing = false;
        this.editId = null;
        this.toast.set(isCreate ? 'Product created.' : 'Product updated.');
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  async remove(p: Product): Promise<void> {
    const ok = await this.confirm.confirm({
      title: 'Delete this product?',
      message: `Delete "${p.name}"? This cannot be undone.`,
      confirmLabel: 'Delete',
    });
    if (!ok) return;
    this.toast.set('');
    this.productsService.delete(p.id).subscribe({
      next: () => {
        this.toast.set(`"${p.name}" deleted.`);
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  categoryName(id: number | null): string {
    const c = this.categories().find((x) => x.id === id);
    return c ? c.name : '—';
  }

  money(value: number): string {
    return value.toFixed(2);
  }

  httpError(err: unknown): string {
    const anyErr = err as { error?: { message?: string; error?: string } };
    return anyErr?.error?.message || anyErr?.error?.error || 'Request failed.';
  }

  image(p: Product): string {
    return p.imageUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><rect width="100%25" height="100%25" fill="%23e8ebee"/><text x="50%25" y="52%25" font-size="12" fill="%239aa5b1" text-anchor="middle">No image</text></svg>';
  }
}