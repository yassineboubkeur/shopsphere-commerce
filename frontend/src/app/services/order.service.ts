import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Order } from '../models/models';

export interface OrderItemRequest {
  productId: number;
  productName: string;
  price: number;
  quantity: number;
}

export interface ShippingInfo {
  shippingName: string;
  shippingAddress: string;
  shippingCity: string;
  shippingZip: string;
  shippingPhone: string;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  placeOrder(userId: number, items: OrderItemRequest[], shipping: ShippingInfo): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/api/orders`, { userId, items, ...shipping });
  }

  getOrderById(orderId: number): Observable<Order> {
    return this.http.get<Order>(`${environment.apiUrl}/api/orders/${orderId}`);
  }

  getOrdersByUserId(userId: number): Observable<Order[]> {
    return this.http.get<Order[]>(`${environment.apiUrl}/api/orders/user/${userId}`);
  }

  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${environment.apiUrl}/api/orders`);
  }

  updateStatus(orderId: number, status: string): Observable<Order> {
    return this.http.patch<Order>(`${environment.apiUrl}/api/orders/${orderId}/status`, { status });
  }

  updateOrder(orderId: number, userId: number, shipping: ShippingInfo): Observable<Order> {
    return this.http.put<Order>(`${environment.apiUrl}/api/orders/${orderId}`, shipping, {
      params: { userId: String(userId) },
    });
  }

  deleteOrder(orderId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/orders/${orderId}`, {
      params: { userId: String(userId) },
    });
  }

  confirmOrder(orderId: number): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/api/orders/${orderId}/confirm`, {});
  }
}