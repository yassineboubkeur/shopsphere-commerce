import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { Navbar } from './navbar';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { ThemeService } from '../services/theme.service';
import { AuthUser } from '../models/models';

describe('Navbar', () => {
  const logout = vi.fn();
  const toggleTheme = vi.fn();

  function stubAuth(user: AuthUser | null) {
    return {
      user: signal<AuthUser | null>(user).asReadonly(),
      isLoggedIn: signal(user != null).asReadonly(),
      isAdmin: signal(user?.role === 'ADMIN').asReadonly(),
      logout,
    };
  }

  beforeEach(async () => {
    logout.mockReset();
    toggleTheme.mockReset();
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [
        provideRouter([{ path: '**', component: Navbar }]),
        { provide: AuthService, useValue: stubAuth(null) },
        { provide: CartService, useValue: { count: signal(0).asReadonly() } },
        { provide: ThemeService, useValue: { resolved: signal('light'), toggle: toggleTheme } },
      ],
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(Navbar);
    fixture.detectChanges();
    return fixture;
  }

  it('should create', () => {
    expect(create().componentInstance).toBeTruthy();
  });

  it('should show the brand', () => {
    expect((create().nativeElement as HTMLElement).querySelector('.brand-text')?.textContent).toContain('ShopSphere');
  });

  it('should show Login and Sign up when logged out', () => {
    const el = create().nativeElement as HTMLElement;
    expect(el.textContent).toContain('Login');
    expect(el.textContent).toContain('Sign up');
    expect(el.textContent).not.toContain('Logout');
  });

  it('should show the username and Logout when logged in', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: stubAuth({ id: 1, username: 'alice', email: 'a@b.c', role: 'USER' }),
    });
    const el = create().nativeElement as HTMLElement;
    expect(el.textContent).toContain('alice');
    expect(el.textContent).toContain('Logout');
    expect(el.textContent).not.toContain('Sign up');
  });

  it('should not show the Admin link for a regular user', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: stubAuth({ id: 1, username: 'alice', email: 'a@b.c', role: 'USER' }),
    });
    const el = create().nativeElement as HTMLElement;
    expect(el.textContent).not.toContain('Admin');
  });

  it('should show the Admin link for an admin user', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: stubAuth({ id: 1, username: 'root', email: 'r@b.c', role: 'ADMIN' }),
    });
    const el = create().nativeElement as HTMLElement;
    expect(el.textContent).toContain('Admin');
  });

  it('should show the cart badge with the cart count', () => {
    TestBed.overrideProvider(CartService, { useValue: { count: signal(3).asReadonly() } });
    const el = create().nativeElement as HTMLElement;
    expect(el.querySelector('.cart-badge')?.textContent?.trim()).toBe('3');
  });

  it('should hide the cart badge when the cart is empty', () => {
    const el = create().nativeElement as HTMLElement;
    const badge = el.querySelector('.cart-badge');
    expect(badge?.getAttribute('hidden')).not.toBeNull();
  });

  it('should toggle the theme', () => {
    const el = create().nativeElement as HTMLElement;
    el.querySelector<HTMLButtonElement>('.theme-toggle')?.click();
    expect(toggleTheme).toHaveBeenCalledTimes(1);
  });

  it('should toggle the mobile menu open', () => {
    const fixture = create();
    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('.burger')?.click();
    fixture.detectChanges();
    expect(root.querySelector('header')?.classList.contains('open')).toBe(true);
  });

  it('should call logout when the Logout button is clicked', () => {
    TestBed.overrideProvider(AuthService, {
      useValue: stubAuth({ id: 1, username: 'alice', email: 'a@b.c', role: 'USER' }),
    });
    const el = create().nativeElement as HTMLElement;
    el.querySelector<HTMLButtonElement>('.btn-logout')?.click();
    expect(logout).toHaveBeenCalledTimes(1);
  });
});
