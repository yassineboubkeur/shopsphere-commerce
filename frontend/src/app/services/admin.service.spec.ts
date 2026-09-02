import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { environment } from '../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET all users', () => {
    service.getUsers().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/users`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should PUT to update a user role', () => {
    service.updateRole(3, 'ADMIN').subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/users/3/role`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ role: 'ADMIN' });
    req.flush({});
  });

  it('should DELETE a user', () => {
    service.deleteUser(3).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/users/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
