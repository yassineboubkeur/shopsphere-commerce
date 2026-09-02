import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AnalyticsService } from './analytics.service';
import { environment } from '../../environments/environment';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET total sales', () => {
    service.getTotalSales().subscribe((v) => expect(v).toEqual({ totalSales: 100 }));
    const req = httpMock.expectOne(`${environment.apiUrl}/api/analytics/total-sales`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalSales: 100 });
  });

  it('should GET total orders', () => {
    service.getTotalOrders().subscribe((v) => expect(v).toEqual({ totalOrders: 5 }));
    const req = httpMock.expectOne(`${environment.apiUrl}/api/analytics/total-orders`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalOrders: 5 });
  });

  it('should GET best selling products', () => {
    service.getBestSelling().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/analytics/best-selling`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should GET sales over time', () => {
    service.getSalesOverTime().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/analytics/sales-over-time`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should GET orders by status', () => {
    service.getOrdersByStatus().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/analytics/orders-by-status`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
