import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PaymentRequest {
  orderId: number;
  userId: number;
  amount: number;
  paymentMethod: string;
  cardNumber?: string;
  cardHolder?: string;
  expiryDate?: string;
  cvv?: string;
}

export interface PaymentResult {
  paymentId: number;
  orderId: number;
  status: string;
  transactionId: string | null;
  amount: number;
  paymentMethod: string;
  message: string;
  processedAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);

  process(request: PaymentRequest): Observable<PaymentResult> {
    return this.http.post<PaymentResult>(`${environment.apiUrl}/api/payments/process`, request);
  }

  getResultByOrderId(orderId: number): Observable<PaymentResult> {
    return this.http.get<PaymentResult>(`${environment.apiUrl}/api/payments/result/order/${orderId}`);
  }
}