import { Component, computed, signal } from '@angular/core';
import { ProductCard } from '../components/product-card';
import { Loader } from '../components/loader';
import { Pagination } from '../components/pagination';
import { ProductService } from '../services/product.service';
import { CartService } from '../services/cart.service';
import { Product, ProductCategory } from '../models/models';

@Component({
  selector: 'app-product-list',
  imports: [ProductCard, Loader, Pagination],
  styleUrl: './product-list.css',
  templateUrl: './product-list.html',
})
export class ProductListComponent {
  protected readonly priceRanges = [
    { label: 'Under $25', min: 0, max: 25 },
    { label: '$25 – $100', min: 25, max: 100 },
    { label: '$100 – $200', min: 100, max: 200 },
    { label: '$200 – $500', min: 200, max: 500 },
    { label: 'Over $500', min: 500, max: 999999 },
  ];
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly allProducts = signal<Product[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<ProductCategory[]>([]);
  protected readonly addedName = signal('');
  protected readonly query = signal('');
  protected readonly activeQuery = signal('');
  protected readonly categoryId = signal('');
  protected readonly activeRange = signal('');
  protected readonly minPrice = signal('');
  protected readonly maxPrice = signal('');
  protected readonly filtersActive = signal(false);
  protected readonly sortBy = signal<'featured' | 'price-asc' | 'price-desc' | 'name-asc' | 'newest'>('featured');
  protected readonly viewMode = signal<'grid' | 'list'>('grid');
  protected readonly currentPage = signal(1);
  protected readonly pageSize = 6;
  private timer: ReturnType<typeof setTimeout> | undefined;

  protected readonly pageProducts = computed(() => {
    const all = this.products();
    const start = (this.currentPage() - 1) * this.pageSize;
    return all.slice(start, start + this.pageSize);
  });

  protected readonly totalPages = computed(() => Math.max(1, Math.ceil(this.products().length / this.pageSize)));

  constructor(
    private readonly productService: ProductService,
    private readonly cart: CartService,
  ) {}

  ngOnInit(): void {
    this.load();
    this.productService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => this.categories.set([]),
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.productService.findAll().subscribe({
      next: (products) => {
        this.allProducts.set(products);
        this.loading.set(false);
        this.liveFilter();
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Failed to load products. Is the backend running?');
      },
    });
  }

  onQueryInput(value: string): void {
    this.query.set(value);
    this.schedule();
  }

  onCategoryChange(value: string): void {
    this.categoryId.set(value);
    this.schedule();
  }

  onPriceInput(minPrice: string, maxPrice: string): void {
    this.minPrice.set(minPrice);
    this.maxPrice.set(maxPrice);
    this.schedule();
  }

  onRangeSelect(label: string): void {
    this.activeRange.set(this.activeRange() === label ? '' : label);
    this.minPrice.set('');
    this.maxPrice.set('');
    this.schedule();
  }

  onCustomToggle(): void {
    if (this.activeRange() === 'custom') {
      this.activeRange.set('');
      this.minPrice.set('');
      this.maxPrice.set('');
    } else {
      this.activeRange.set('custom');
    }
    this.schedule();
  }

  search(): void {
    clearTimeout(this.timer);
    this.liveFilter();
  }

  private schedule(): void {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.liveFilter(), 300);
  }

  private liveFilter(): void {
    const q = this.query().trim().toLowerCase();
    const catId = this.categoryId();
    const range = this.priceRanges.find((r) => r.label === this.activeRange());

    let hasPrice = false;
    let lo = 0;
    let hi = 999999;
    if (range) {
      hasPrice = true;
      lo = range.min;
      hi = range.max;
    } else if (this.activeRange() === 'custom') {
      const rawMin = this.minPrice().trim();
      const rawMax = this.maxPrice().trim();
      hasPrice = rawMin !== '' || rawMax !== '';
      const min = rawMin === '' ? 0 : parseFloat(rawMin);
      const max = rawMax === '' ? 999999 : parseFloat(rawMax);
      [lo, hi] = min <= max ? [min, max] : [max, min];
    }

    this.filtersActive.set(catId !== '' || hasPrice);
    this.activeQuery.set(q ? this.query().trim() : '');

    const result = this.allProducts().filter((p) => {
      const nameOk = !q || p.name.toLowerCase().includes(q);
      const catOk = catId === '' || String(p.category?.id) === catId;
      const priceOk = !hasPrice || (p.price >= lo && p.price <= hi);
      return nameOk && catOk && priceOk;
    });

    this.products.set(this.sort(result));
    this.currentPage.set(1);
  }

  private sort(list: Product[]): Product[] {
    const sorted = [...list];
    switch (this.sortBy()) {
      case 'price-asc':
        sorted.sort((a, b) => a.price - b.price);
        break;
      case 'price-desc':
        sorted.sort((a, b) => b.price - a.price);
        break;
      case 'name-asc':
        sorted.sort((a, b) => a.name.localeCompare(b.name));
        break;
      case 'newest':
        sorted.sort((a, b) => b.id - a.id);
        break;
      default:
        sorted.sort((a, b) => a.id - b.id);
    }
    return sorted;
  }

  clearSearch(): void {
    clearTimeout(this.timer);
    this.query.set('');
    this.activeQuery.set('');
    this.liveFilter();
  }

  clearFilters(): void {
    clearTimeout(this.timer);
    this.categoryId.set('');
    this.activeRange.set('');
    this.minPrice.set('');
    this.maxPrice.set('');
    this.filtersActive.set(false);
    this.query.set('');
    this.activeQuery.set('');
    this.liveFilter();
  }

  countFor(categoryId: number): number {
    return this.allProducts().filter((p) => p.category?.id === categoryId).length;
  }

  catKey(categoryId: number): string {
    return String(categoryId);
  }

  onSortChange(value: string): void {
    this.sortBy.set(value as 'featured' | 'price-asc' | 'price-desc' | 'name-asc' | 'newest');
    this.liveFilter();
  }

  onViewMode(mode: 'grid' | 'list'): void {
    this.viewMode.set(mode);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  addToCart(product: Product): void {
    this.cart.add(product);
    this.addedName.set(product.name);
    setTimeout(() => this.addedName.set(''), 2000);
  }
}