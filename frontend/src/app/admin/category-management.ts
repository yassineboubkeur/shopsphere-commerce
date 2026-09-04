import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductService, CategoryRequest } from '../services/product.service';
import { Product, ProductCategory } from '../models/models';
import { ConfirmDialogService } from '../services/confirm-dialog.service';

@Component({
  selector: 'app-category-management',
  imports: [FormsModule],
  styleUrl: './category-management.css',
  templateUrl: './category-management.html',
})
export class CategoryManagementComponent {
  private readonly productsService = inject(ProductService);
  private readonly confirm = inject(ConfirmDialogService);

  protected readonly categories = signal<ProductCategory[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly saving = signal(false);
  protected readonly toast = signal('');
  protected editing = false;
  protected editId: number | null = null;

  protected name = '';
  protected description = '';
  protected imageUrl = '';

  private products: Product[] = [];
  protected readonly productCount = new Map<number, number>();

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.productsService.getCategories().subscribe({
      next: (c) => {
        this.categories.set(c);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
    this.productsService.findAll().subscribe({
      next: (p) => {
        this.products = p;
        this.productCount.clear();
        for (const prod of p) {
          const id = prod.category?.id ?? -1;
          this.productCount.set(id, (this.productCount.get(id) ?? 0) + 1);
        }
      },
    });
  }

  openNew(): void {
    this.editing = true;
    this.editId = null;
    this.name = '';
    this.description = '';
    this.imageUrl = '';
  }

  openEdit(c: ProductCategory): void {
    this.editing = true;
    this.editId = c.id;
    this.name = c.name;
    this.description = c.description ?? '';
    this.imageUrl = c.imageUrl ?? '';
  }

  cancel(): void {
    this.editing = false;
    this.editId = null;
  }

  save(): void {
    if (!this.name.trim()) {
      this.toast.set('Category name is required.');
      setTimeout(() => this.toast.set(''), 3500);
      return;
    }
    const request: CategoryRequest = {
      name: this.name.trim(),
      description: this.description.trim(),
      imageUrl: this.imageUrl.trim(),
    };
    this.saving.set(true);
    this.toast.set('');
    const isCreate = this.editId === null;
    const call$ = isCreate
      ? this.productsService.createCategory(request)
      : this.productsService.updateCategory(this.editId!, request);
    call$.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing = false;
        this.editId = null;
        this.toast.set(isCreate ? 'Category created.' : 'Category updated.');
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

  async remove(c: ProductCategory): Promise<void> {
    const count = this.productCount.get(c.id) ?? 0;
    const ok = await this.confirm.confirm({
      title: count > 0 ? 'Delete category with products?' : 'Delete this category?',
      message: count > 0
        ? `Delete "${c.name}"? This has ${count} product(s) — they will become uncategorized. This cannot be undone.`
        : `Delete "${c.name}"? This cannot be undone.`,
      confirmLabel: 'Delete',
    });
    if (!ok) return;
    this.toast.set('');
    this.productsService.deleteCategory(c.id).subscribe({
      next: () => {
        this.toast.set(`"${c.name}" deleted.`);
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  count(c: ProductCategory): number {
    return this.productCount.get(c.id) ?? 0;
  }

  image(c: ProductCategory): string {
    return c.imageUrl || 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80"><rect width="100%25" height="100%25" fill="%23e8ebee"/><text x="50%25" y="52%25" font-size="11" fill="%239aa5b1" text-anchor="middle">No image</text></svg>';
  }

  httpError(err: unknown): string {
    const anyErr = err as { error?: { message?: string; error?: string } };
    return anyErr?.error?.message || anyErr?.error?.error || 'Request failed.';
  }
}