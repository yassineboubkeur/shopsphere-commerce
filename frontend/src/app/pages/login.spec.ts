import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login';
import { AuthService } from '../services/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let auth: {
    login: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  function setup(admin = false) {
    auth = {
      login: vi.fn().mockReturnValue(of(null)),
      isAdmin: vi.fn().mockReturnValue(admin),
    };
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    fixture = TestBed.createComponent(LoginComponent);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  function setField(selector: string, value: string) {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function clickSubmit() {
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  it('should be created', () => {
    setup();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show a required error and not call login when the form is empty', () => {
    setup();
    clickSubmit();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('A valid email is required.');
    expect(el.textContent).toContain('Password is required.');
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('should call login and navigate to /products for a normal user', () => {
    setup(false);
    const navigate = vi.fn();
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    setField('input[formcontrolname="email"]', 'a@b.c');
    setField('input[formcontrolname="password"]', 'secret');
    clickSubmit();

    expect(auth.login).toHaveBeenCalledWith('a@b.c', 'secret');
    expect(navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should navigate to /admin/dashboard for an admin', () => {
    setup(true);
    const navigate = vi.fn();
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    setField('input[formcontrolname="email"]', 'root@b.c');
    setField('input[formcontrolname="password"]', 'admin');
    clickSubmit();

    expect(navigate).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('should display the returned error message and not navigate', () => {
    setup(false);
    auth.login.mockReturnValue(of('Invalid email or password.'));
    const navigate = vi.fn();
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    setField('input[formcontrolname="email"]', 'a@b.c');
    setField('input[formcontrolname="password"]', 'wrong');
    clickSubmit();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Invalid email or password.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should show a signing-in label while loading', () => {
    setup(false);
    fixture.componentInstance['loading'].set(true);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.textContent?.trim()).toBe('Signing in...');
  });
});
