import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { RegisterComponent } from './register';
import { AuthService } from '../services/auth.service';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let auth: {
    register: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  function setup() {
    auth = {
      register: vi.fn().mockReturnValue(of(null)),
      isAdmin: vi.fn().mockReturnValue(false),
    };
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    fixture = TestBed.createComponent(RegisterComponent);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  const validPassword = 'Abcd1234!';

  function setField(selector: string, value: string) {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function fillValid() {
    setField('input[formcontrolname="username"]', 'alice');
    setField('input[formcontrolname="email"]', 'a@b.c');
    setField('input[formcontrolname="password"]', validPassword);
    setField('input[formcontrolname="confirmPassword"]', validPassword);
  }

  function clickSubmit() {
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  it('should be created', () => {
    setup();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show the password policy rules once the password field is touched', () => {
    setup();
    setField('input[formcontrolname="password"]', 'password123');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('An uppercase letter (A–Z)');
  });

  it('should show an error when passwords do not match and not register', () => {
    setup();
    setField('input[formcontrolname="username"]', 'alice');
    setField('input[formcontrolname="email"]', 'a@b.c');
    setField('input[formcontrolname="password"]', validPassword);
    setField('input[formcontrolname="confirmPassword"]', 'Different1!');
    clickSubmit();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Passwords do not match.');
    expect(auth.register).not.toHaveBeenCalled();
  });

  it('should register and navigate to /products on success', () => {
    setup();
    const navigate = vi.fn();
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    fillValid();
    clickSubmit();

    expect(auth.register).toHaveBeenCalledWith('alice', 'a@b.c', validPassword);
    expect(navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should display the returned error message and not navigate', () => {
    setup();
    auth.register.mockReturnValue(of('This email is already registered.'));
    const navigate = vi.fn();
    vi.spyOn(router, 'navigate').mockImplementation(navigate as never);

    fillValid();
    clickSubmit();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('This email is already registered.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should reveal the password when toggled', () => {
    setup();
    const input = fixture.nativeElement.querySelector('input[formcontrolname="password"]') as HTMLInputElement;
    expect(input.type).toBe('password');
    const toggle = fixture.nativeElement.querySelector('.password-wrap .toggle') as HTMLButtonElement;
    toggle.click();
    fixture.detectChanges();
    expect((input as unknown as HTMLInputElement).type).toBe('text');
  });
});
