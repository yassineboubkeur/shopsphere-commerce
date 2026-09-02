import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { adminGuard } from './admin.guard';

function stubAuthService(loggedIn: boolean, admin: boolean) {
  return {
    isLoggedIn: () => loggedIn,
    isAdmin: () => admin,
    getToken: () => (loggedIn ? 'TOKEN' : null),
    logout: () => undefined,
  } as unknown as AuthService;
}

function makeState(url: string): RouterStateSnapshot {
  return { url } as RouterStateSnapshot;
}

describe('authGuard', () => {
  it('should allow access when the user is logged in', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: stubAuthService(true, false) },
      ],
    });
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, makeState('/products')),
    );
    expect(result).toBe(true);
  });

  it('should redirect to login with returnUrl when logged out', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: stubAuthService(false, false) },
      ],
    });
    const router = TestBed.inject(Router);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, makeState('/checkout')),
    );
    expect(router.serializeUrl(result as never)).toBe('/login?returnUrl=%2Fcheckout');
  });
});

describe('adminGuard', () => {
  it('should allow access for an admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: stubAuthService(true, true) },
      ],
    });
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, makeState('/admin/dashboard')),
    );
    expect(result).toBe(true);
  });

  it('should redirect a non-admin logged-in user to /products', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: stubAuthService(true, false) },
      ],
    });
    const router = TestBed.inject(Router);
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, makeState('/admin/dashboard')),
    );
    expect(router.serializeUrl(result as never)).toBe('/products');
  });

  it('should redirect a logged-out user to login with returnUrl', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: stubAuthService(false, false) },
      ],
    });
    const router = TestBed.inject(Router);
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, makeState('/admin')),
    );
    expect(router.serializeUrl(result as never)).toBe('/login?returnUrl=%2Fadmin');
  });
});
