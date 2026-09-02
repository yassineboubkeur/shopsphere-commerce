import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    window.localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created and logged out by default', () => {
    expect(service).toBeTruthy();
    expect(service.isLoggedIn()).toBe(false);
    expect(service.isAdmin()).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('should log in and store the session', () => {
    service.login('a@b.c', 'secret').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.c', password: 'secret' });
    req.flush({ token: 'TOKEN-1', type: 'Bearer', id: 1, username: 'alice', email: 'a@b.c', role: 'ROLE_USER' });

    expect(service.getToken()).toBe('TOKEN-1');
    expect(service.isLoggedIn()).toBe(true);
    expect(service.user()?.username).toBe('alice');
    expect(service.isAdmin()).toBe(false);
  });

  it('should mark an ADMIN user as admin', () => {
    service.login('root@b.c', 'secret').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    req.flush({ token: 'T', type: 'Bearer', id: 2, username: 'root', email: 'root@b.c', role: 'ROLE_ADMIN' });
    expect(service.isAdmin()).toBe(true);
  });

  it('should return an error message on invalid credentials', () => {
    let result: string | null = '';
    service.login('a@b.c', 'wrong').subscribe((r) => (result = r));
    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    req.flush({ message: 'Bad credentials' }, { status: 401, statusText: 'Unauthorized' });
    expect(result).toBe('Invalid email or password.');
  });

  it('should register a user and store the session', () => {
    service.register('bob', 'b@b.c', 'secret').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'bob', email: 'b@b.c', password: 'secret' });
    req.flush({ token: 'T2', type: 'Bearer', id: 3, username: 'bob', email: 'b@b.c', role: 'ROLE_USER' });
    expect(service.getToken()).toBe('T2');
    expect(service.user()?.username).toBe('bob');
  });

  it('should fetch the profile and set it', () => {
    service.getProfile().subscribe((u) => {
      expect(u?.username).toBe('carol');
      expect(service.isLoggedIn()).toBe(true);
    });
    const req = httpMock.expectOne(`${environment.apiUrl}/api/user/profile`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 4, username: 'carol', email: 'c@b.c', role: 'ROLE_USER' });
  });

  it('should return null from getProfile on error', () => {
    let result: unknown = 'unset';
    service.getProfile().subscribe((u) => (result = u));
    const req = httpMock.expectOne(`${environment.apiUrl}/api/user/profile`);
    req.flush({ message: 'No user' }, { status: 401, statusText: 'Unauthorized' });
    expect(result).toBeNull();
  });

  it('should log out and clear the session', () => {
    service.login('a@b.c', 'secret').subscribe();
    const loginReq = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    loginReq.flush({ token: 'T', type: 'Bearer', id: 1, username: 'alice', email: 'a@b.c', role: 'ROLE_USER' });

    service.logout();
    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
    expect(window.localStorage.getItem('user')).toBeNull();
  });
});
