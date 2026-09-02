import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentService, PaymentRequest } from './payment.service';
import { environment } from '../../environments/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST a payment request to process', () => {
    const request: PaymentRequest = {
      orderId: 9,
      userId: 4,
      amount: 59.99,
      paymentMethod: 'STRIPE',
    };
    service.process(request).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/payments/process`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('should GET the payment result by order id', () => {
    service.getResultByOrderId(9).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/payments/result/order/9`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });
});
