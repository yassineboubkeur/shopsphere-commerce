import { Component, computed, inject, signal } from '@angular/core';
import { timeout } from 'rxjs';
import {
  AnalyticsService,
  SalesOverTime,
  OrdersByStatus,
} from '../services/analytics.service';

interface BestSeller {
  productName: string;
  totalQuantity: number;
}

@Component({
  selector: 'app-statistics',
  styleUrl: './statistics.css',
  templateUrl: './statistics.html',
})
export class StatisticsComponent {
  private readonly analytics = inject(AnalyticsService);

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  protected readonly totalSales = signal<number>(0);
  protected readonly totalOrders = signal<number>(0);
  protected readonly bestSelling = signal<BestSeller[]>([]);
  protected readonly ordersByStatus = signal<OrdersByStatus[]>([]);
  protected readonly salesOverTime = signal<SalesOverTime[]>([]);

  protected readonly maxQuantity = computed(() =>
    Math.max(1, ...this.bestSelling().map((p) => p.totalQuantity)),
  );

  protected readonly maxStatusCount = computed(() =>
    Math.max(1, ...this.ordersByStatus().map((o) => o.count)),
  );

  constructor() {
    this.load();
  }

  private settled = 0;

  load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.settled = 0;

    this.analytics.getTotalSales().pipe(timeout(10000)).subscribe({
      next: (r) => {
        this.totalSales.set(Number(r.totalSales) || 0);
        this.settle();
      },
      error: () => this.settle(),
    });
    this.analytics.getTotalOrders().pipe(timeout(10000)).subscribe({
      next: (r) => {
        this.totalOrders.set(Number(r.totalOrders) || 0);
        this.settle();
      },
      error: () => this.settle(),
    });
    this.analytics.getBestSelling().pipe(timeout(10000)).subscribe({
      next: (r) => this.settle(() => this.bestSelling.set(r || [])),
      error: () => this.settle(),
    });
    this.analytics.getOrdersByStatus().pipe(timeout(10000)).subscribe({
      next: (r) => this.settle(() => this.ordersByStatus.set(r || [])),
      error: () => this.settle(),
    });
    this.analytics.getSalesOverTime().pipe(timeout(10000)).subscribe({
      next: (r) => this.settle(() => this.salesOverTime.set(r || [])),
      error: () => this.settle(),
    });

    setTimeout(() => this.loading.set(false), 12000);
  }

  private settle(mutate?: () => void): void {
    if (mutate) mutate();
    this.settled++;
    if (this.settled === 5) {
      this.loading.set(false);
    }
  }

  money(value: number): string {
    return (value || 0).toFixed(2);
  }

  statusPct(order: OrdersByStatus): number {
    return Math.round((order.count / this.maxStatusCount()) * 100);
  }

  quantityPct(product: BestSeller): number {
    return Math.round((product.totalQuantity / this.maxQuantity()) * 100);
  }
}
