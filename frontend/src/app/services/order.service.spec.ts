import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderService, OrderItemRequest, ShippingInfo } from './order.service';
import { environment } from '../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  const shipping: ShippingInfo = {
    shippingName: 'Alice',
    shippingAddress: '1 Main St',
    shippingCity: 'Casablanca',
    shippingZip: '20000',
    shippingPhone: '0612345678',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST a new order with user, items and shipping', () => {
    const items: OrderItemRequest[] = [{ productId: 1, productName: 'Jacket', price: 20, quantity: 2 }];
    service.placeOrder(4, items, shipping).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toMatchObject({ userId: 4, items, shippingName: 'Alice' });
    req.flush({});
  });

  it('should GET an order by id', () => {
    service.getOrderById(9).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders/9`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should GET orders by user id', () => {
    service.getOrdersByUserId(4).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders/user/4`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should GET all orders', () => {
    service.getAllOrders().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should PATCH the order status', () => {
    service.updateStatus(9, 'SHIPPED').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders/9/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'SHIPPED' });
    req.flush({});
  });

  it('should PUT order shipping info with userId query param', () => {
    service.updateOrder(9, 4, shipping).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders/9?userId=4`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.params.get('userId')).toBe('4');
    req.flush({});
  });

  it('should DELETE an order with userId query param', () => {
    service.deleteOrder(9, 4).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders/9?userId=4`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should POST to confirm an order', () => {
    service.confirmOrder(9).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/orders/9/confirm`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
