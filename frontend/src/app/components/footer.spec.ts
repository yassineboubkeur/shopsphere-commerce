import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Footer } from './footer';

describe('Footer', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Footer], providers: [provideRouter([])] }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(Footer);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show the current year', () => {
    const fixture = TestBed.createComponent(Footer);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(String(new Date().getFullYear()));
  });

  it('should include the brand name', () => {
    const fixture = TestBed.createComponent(Footer);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ShopSphere');
  });
});
