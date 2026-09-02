import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';
import { environment } from '../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;

  function configure(authStub: Partial<AuthService>) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authStub },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  }

  afterEach(() => httpMock.verify());

  it('should attach the bearer token when one exists', () => {
    configure({ getToken: () => 'SECRET' } as Partial<AuthService>);
    http.get(`${environment.apiUrl}/api/products`).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer SECRET');
    req.flush([]);
  });

  it('should not attach an authorization header when there is no token', () => {
    configure({ getToken: () => null } as Partial<AuthService>);
    http.get(`${environment.apiUrl}/api/products`).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/products`);
    expect(req.request.headers.get('Authorization')).toBeNull();
    req.flush([]);
  });

  it('should log out and navigate to /login on a 401 (non-login request)', () => {
    const logout = vi.fn();
    const navigate = vi.fn();
    configure({ getToken: () => 'SECRET', logout } as unknown as Partial<AuthService>);
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    let error: unknown;
    http.get(`${environment.apiUrl}/api/orders`).subscribe({ error: (e) => (error = e) });
    httpMock.expectOne(`${environment.apiUrl}/api/orders`).flush(
      { message: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(logout).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(error).toBeTruthy();
  });

  it('should not log out on a 401 from the login endpoint', () => {
    const logout = vi.fn();
    const navigate = vi.fn();
    configure({ getToken: () => null, logout } as unknown as Partial<AuthService>);
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    let error: unknown;
    http
      .post(`${environment.apiUrl}/api/auth/login`, { email: 'a@b.c', password: 'x' })
      .subscribe({ error: (e) => (error = e) });
    httpMock
      .expectOne(`${environment.apiUrl}/api/auth/login`)
      .flush({ message: 'Bad' }, { status: 401, statusText: 'Unauthorized' });

    expect(logout).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    expect(error).toBeTruthy();
  });
});
