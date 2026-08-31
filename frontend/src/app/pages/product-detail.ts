import { Component, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Loader } from '../components/loader';
import { ProductService } from '../services/product.service';
import { CartService } from '../services/cart.service';
import { Product } from '../models/models';

@Component({
  selector: 'app-product-detail',
  imports: [RouterLink, Loader],
  styleUrl: './product-detail.css',
  templateUrl: './product-detail.html',
})
export class ProductDetailComponent {
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly product = signal<Product | null>(null);
  protected readonly quantity = signal(1);
  protected readonly added = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly productService: ProductService,
    private readonly cart: CartService,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      this.load(id);
    });
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.product.set(null);
    this.quantity.set(1);
    this.added.set(false);
    this.productService.findById(id).subscribe({
      next: (p) => {
        this.product.set(p);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.status === 404 ? 'Product not found.' : 'Failed to load the product. Is the backend running?');
      },
    });
  }

  setQuantity(v: number): void {
    const product = this.product();
    this.quantity.set(Math.max(1, Math.min(v || 1, product?.stockQuantity ?? 1)));
  }

  decrease(): void {
    if (this.quantity() > 1) this.setQuantity(this.quantity() - 1);
  }

  increase(): void {
    this.setQuantity(this.quantity() + 1);
  }

  addToCart(): void {
    const product = this.product();
    if (!product) return;
    this.cart.add(product, this.quantity());
    this.added.set(true);
    setTimeout(() => this.added.set(false), 2000);
  }

  priceLabel(): string {
    return (this.product()?.price ?? 0).toFixed(2);
  }
}