import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductService, ProductRequest, CategoryRequest } from './product.service';
import { environment } from '../../environments/environment';
import { Product } from '../models/models';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET all products', () => {
    const expected: Product[] = [];
    service.findAll().subscribe((p) => expect(p).toEqual(expected));
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products`);
    expect(req.request.method).toBe('GET');
    req.flush(expected);
  });

  it('should GET categories', () => {
    service.getCategories().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/categories`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should GET paginated products with query params', () => {
    service.findPaginated(2, 10, 'name', 'desc').subscribe();
    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/products/paginated?page=2&size=10&sortBy=name&direction=desc`,
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalPages: 1, totalElements: 0, number: 0, size: 10, empty: true });
  });

  it('should GET a product by id', () => {
    service.findById(42).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products/42`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should search by name', () => {
    service.searchByName('denim jacket').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products/search?name=denim%20jacket`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should filter by price', () => {
    service.filterByPrice(10, 50).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products/filter/price?minPrice=10&maxPrice=50`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should POST a new product', () => {
    const request: ProductRequest = {
      name: 'Jacket',
      description: 'd',
      price: 10,
      stockQuantity: 5,
      imageUrl: '',
      categoryId: 1,
    };
    service.create(request).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('should PUT an existing product', () => {
    service.update(7, {} as ProductRequest).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products/7`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should DELETE a product', () => {
    service.delete(7).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should PATCH product stock', () => {
    service.updateStock(7, 25).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products/7/stock`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ quantity: 25 });
    req.flush({});
  });

  it('should POST a new category', () => {
    const request: CategoryRequest = { name: 'Clothing', description: 'd', imageUrl: '' };
    service.createCategory(request).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/categories`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('should DELETE a category', () => {
    service.deleteCategory(3).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/categories/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
