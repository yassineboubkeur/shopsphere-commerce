import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductCard } from '../components/product-card';
import { Loader } from '../components/loader';
import { ProductService } from '../services/product.service';
import { CartService } from '../services/cart.service';
import { Product, ProductCategory } from '../models/models';

@Component({
  selector: 'app-home',
  imports: [ProductCard, Loader, RouterLink],
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class HomeComponent {
  private readonly productService = inject(ProductService);
  private readonly cart = inject(CartService);

  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly categories = signal<ProductCategory[]>([]);
  protected readonly featured = signal<Product[]>([]);
  protected readonly addedName = signal('');

  protected readonly hero = {
    badge: 'New Season · Up to 40% off',
    title: 'Discover What You Love',
    subtitle:
      'Explore a curated collection of quality products at the best prices. Fast delivery, secure checkout, and effortless returns.',
    primaryCta: 'Shop Now',
    primaryUrl: '/products',
    secondaryCta: 'Browse Categories',
    secondaryUrl: '#categories',
  };

  protected readonly features = [
    { icon: 'truck', title: 'Free Shipping', text: 'On all orders over $50' },
    { icon: 'shield', title: 'Secure Payment', text: 'Your data is always protected' },
    { icon: 'rotate', title: 'Easy Returns', text: '30-day money back guarantee' },
    { icon: 'headset', title: '24/7 Support', text: 'We are here to help you' },
  ];

  protected readonly stats = [
    { value: '1M+', label: 'Happy Customers' },
    { value: '25K+', label: 'Products' },
    { value: '120+', label: 'Countries Served' },
    { value: '4.9/5', label: 'Average Rating' },
  ];

  ngOnInit(): void {
    this.productService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => this.categories.set([]),
    });

    this.productService.findAll().subscribe({
      next: (products) => {
        const active = products.filter((p) => p.active);
        const featured = [...active].sort((a, b) => b.id - a.id).slice(0, 8);
        this.featured.set(featured);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Failed to load products. Is the backend running?');
      },
    });
  }

  addToCart(product: Product): void {
    this.cart.add(product);
    this.addedName.set(product.name);
    setTimeout(() => this.addedName.set(''), 2000);
  }

  onSubmitSubscribe(email: string): void {
    if (!email.trim()) return;
    this.subscribed.set(true);
    this.newsletterEmail.set('');
    setTimeout(() => this.subscribed.set(false), 4000);
  }

  protected readonly subscribed = signal(false);
  protected readonly newsletterEmail = signal('');
}
