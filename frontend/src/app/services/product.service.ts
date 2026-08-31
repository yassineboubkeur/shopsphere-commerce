import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Product, ProductCategory, SpringPage } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Product[]> {
    return this.http.get<Product[]>(`${environment.apiUrl}/api/products`);
  }

  getCategories(): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${environment.apiUrl}/api/categories`);
  }

  findPaginated(page: number, size: number, sortBy = 'id', direction = 'asc'): Observable<SpringPage<Product>> {
    return this.http.get<SpringPage<Product>>(
      `${environment.apiUrl}/api/products/paginated?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`,
    );
  }

  findById(id: number): Observable<Product> {
    return this.http.get<Product>(`${environment.apiUrl}/api/products/${id}`);
  }

  searchByName(name: string): Observable<Product[]> {
    return this.http.get<Product[]>(`${environment.apiUrl}/api/products/search?name=${encodeURIComponent(name)}`);
  }

  findByCategory(categoryId: number): Observable<Product[]> {
    return this.http.get<Product[]>(`${environment.apiUrl}/api/products/category/${categoryId}`);
  }

  filterByPrice(minPrice: number, maxPrice: number): Observable<Product[]> {
    return this.http.get<Product[]>(
      `${environment.apiUrl}/api/products/filter/price?minPrice=${minPrice}&maxPrice=${maxPrice}`,
    );
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(`${environment.apiUrl}/api/products`, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${environment.apiUrl}/api/products/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/products/${id}`);
  }

  updateStock(id: number, quantity: number): Observable<Product> {
    return this.http.patch<Product>(`${environment.apiUrl}/api/products/${id}/stock`, { quantity });
  }

  createCategory(request: CategoryRequest): Observable<ProductCategory> {
    return this.http.post<ProductCategory>(`${environment.apiUrl}/api/categories`, request);
  }

  updateCategory(id: number, request: CategoryRequest): Observable<ProductCategory> {
    return this.http.put<ProductCategory>(`${environment.apiUrl}/api/categories/${id}`, request);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/categories/${id}`);
  }
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  imageUrl: string;
  categoryId: number;
}

export interface CategoryRequest {
  name: string;
  description: string;
  imageUrl: string;
}