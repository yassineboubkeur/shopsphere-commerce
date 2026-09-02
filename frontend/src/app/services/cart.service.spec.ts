import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { Product } from '../models/models';

function makeProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: 'Denim Jacket',
    description: '',
    price: 59.99,
    stockQuantity: 10,
    imageUrl: null,
    active: true,
    category: null,
    ...overrides,
  };
}

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  it('should start empty', () => {
    expect(service.items()).toEqual([]);
    expect(service.count()).toBe(0);
    expect(service.total()).toBe(0);
  });

  it('should add a product to the cart', () => {
    const p = makeProduct();
    service.add(p, 2);
    expect(service.items()).toHaveLength(1);
    expect(service.count()).toBe(2);
    expect(service.total()).toBeCloseTo(119.98, 2);
  });

  it('should merge quantities when adding the same product', () => {
    const p = makeProduct();
    service.add(p, 2);
    service.add(p, 3);
    expect(service.items()).toHaveLength(1);
    expect(service.count()).toBe(5);
  });

  it('should cap quantity at the product stock', () => {
    const p = makeProduct({ stockQuantity: 4 });
    service.add(p, 2);
    service.add(p, 5);
    expect(service.items()[0].quantity).toBe(4);
  });

  it('should set the quantity of a specific product', () => {
    const p = makeProduct();
    service.add(p, 5);
    service.setQuantity(p.id, 3);
    expect(service.items()[0].quantity).toBe(3);
  });

  it('should remove a product from the cart', () => {
    service.add(makeProduct(), 1);
    service.add(makeProduct({ id: 2, name: 'T-Shirt' }), 1);
    service.remove(1);
    expect(service.items()).toHaveLength(1);
    expect(service.items()[0].product.id).toBe(2);
  });

  it('should clear the cart', () => {
    service.add(makeProduct(), 1);
    service.clear();
    expect(service.items()).toEqual([]);
  });

  it('should persist the cart to localStorage', () => {
    service.add(makeProduct({ id: 9 }), 1);
    const raw = window.localStorage.getItem('cart');
    expect(raw).toBeTruthy();
    const parsed = JSON.parse(raw as string);
    expect(parsed[0].product.id).toBe(9);
  });

  it('should restore items from localStorage on creation', () => {
    window.localStorage.setItem(
      'cart',
      JSON.stringify([{ product: makeProduct({ id: 5 }), quantity: 2 }]),
    );
    TestBed.resetTestingModule();
    service = TestBed.inject(CartService);
    expect(service.count()).toBe(2);
    expect(service.items()[0].product.id).toBe(5);
  });
});
