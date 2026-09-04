import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Notification } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/api/notifications`);
  }

  getByUser(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/api/notifications/user/${userId}`);
  }

  getById(id: number): Observable<Notification> {
    return this.http.get<Notification>(`${environment.apiUrl}/api/notifications/${id}`);
  }

  getByUserAndType(userId: number, type: string): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/api/notifications/user/${userId}/type/${type}`);
  }

  getShippingNotifications(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/api/notifications/shipping/user/${userId}`);
  }

  getOrderConfirmations(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/api/notifications/order-confirmation/user/${userId}`);
  }

  getPaymentConfirmations(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/api/notifications/payment-confirmation/user/${userId}`);
  }

  markAsRead(id: number): Observable<Notification> {
    return this.http.patch<Notification>(`${environment.apiUrl}/api/notifications/${id}/status`, { status: 'SEEN' });
  }
}