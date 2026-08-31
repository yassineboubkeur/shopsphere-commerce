import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SalesOverTime {
  date: string;
  totalSales: number;
}

export interface OrdersByStatus {
  status: string;
  count: number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  getTotalSales(): Observable<{ totalSales: number }> {
    return this.http.get<{ totalSales: number }>(`${environment.apiUrl}/api/analytics/total-sales`);
  }

  getTotalOrders(): Observable<{ totalOrders: number }> {
    return this.http.get<{ totalOrders: number }>(`${environment.apiUrl}/api/analytics/total-orders`);
  }

  getBestSelling(): Observable<{ productName: string; totalQuantity: number }[]> {
    return this.http.get<{ productName: string; totalQuantity: number }[]>(`${environment.apiUrl}/api/analytics/best-selling`);
  }

  getSalesOverTime(): Observable<SalesOverTime[]> {
    return this.http.get<SalesOverTime[]>(`${environment.apiUrl}/api/analytics/sales-over-time`);
  }

  getOrdersByStatus(): Observable<OrdersByStatus[]> {
    return this.http.get<OrdersByStatus[]>(`${environment.apiUrl}/api/analytics/orders-by-status`);
  }
}
