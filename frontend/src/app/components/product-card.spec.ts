import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProductCard } from './product-card';
import { Product } from '../models/models';

function makeProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: 'Denim Jacket',
    description: 'Classic denim jacket',
    price: 59.99,
    stockQuantity: 10,
    imageUrl: null,
    active: true,
    category: { id: 1, name: 'Clothing', description: '', imageUrl: null },
    ...overrides,
  };
}

describe('ProductCard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductCard],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  function create(overrides: { product?: Product; showActions?: boolean } = {}) {
    const fixture = TestBed.createComponent(ProductCard);
    const component = fixture.componentInstance;
    const product = overrides.product ?? makeProduct();
    fixture.componentRef.setInput('product', product);
    fixture.componentRef.setInput('showActions', overrides.showActions ?? false);
    fixture.detectChanges();
    return { fixture, component, product };
  }

  it('should create', () => {
    const { component } = create();
    expect(component).toBeTruthy();
  });

  it('should display product name, category, description, and price', () => {
    const { fixture } = create();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.name')?.textContent).toContain('Denim Jacket');
    expect(el.querySelector('.category')?.textContent).toContain('Clothing');
    expect(el.querySelector('.description')?.textContent).toContain('Classic denim jacket');
    expect(el.querySelector('.price')?.textContent).toContain('$59.99');
  });

  it('should show stock count when in stock', () => {
    const { fixture } = create();
    expect((fixture.nativeElement as HTMLElement).querySelector('.stock')?.textContent).toContain('10 in stock');
  });

  it('should show Out of stock and disable button when stock is 0', () => {
    const { fixture } = create({ product: makeProduct({ stockQuantity: 0 }) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.stock')?.textContent).toContain('Out of stock');
    expect(el.querySelector<HTMLButtonElement>('.btn-primary')?.disabled).toBe(true);
  });

  it('should render placeholder with the first letter when no image', () => {
    const { fixture } = create();
    expect((fixture.nativeElement as HTMLElement).querySelector('.placeholder-image')?.textContent).toBe('D');
  });

  it('should render an image when imageUrl is provided', () => {
    const { fixture } = create({ product: makeProduct({ imageUrl: 'http://x/jacket.jpg' }) });
    const img = (fixture.nativeElement as HTMLElement).querySelector<HTMLImageElement>('img.image');
    expect(img?.getAttribute('src')).toContain('jacket.jpg');
  });

  it('should label button Add to cart and emit addToCart on click', () => {
    const { fixture, component, product } = create();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.btn-primary')?.textContent).toContain('Add to cart');
    let emitted: Product | undefined;
    component.addToCart.subscribe((p) => (emitted = p));
    el.querySelector<HTMLButtonElement>('.btn-primary')?.click();
    expect(emitted).toEqual(product);
  });

  it('should show Edit button and label Add when showActions is true', () => {
    const { fixture, component, product } = create({ showActions: true });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.btn-primary')?.textContent).toContain('Add');
    let emitted: Product | undefined;
    component.edit.subscribe((p) => (emitted = p));
    el.querySelector<HTMLButtonElement>('button.btn:not(.btn-primary)')?.click();
    expect(emitted).toEqual(product);
  });

  it('should format the price to two decimals', () => {
    const { component } = create();
    expect(component.priceLabel()).toBe('59.99');
  });
});
