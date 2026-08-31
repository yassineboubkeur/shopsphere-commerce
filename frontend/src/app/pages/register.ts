import { Component, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  PASSWORD_MAX,
  evaluatePassword,
  passwordPolicyValidator,
  strengthLabel,
  strengthPct,
} from '../validators/password.policy';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrls: ['./login.css', './register.css'],
  templateUrl: './register.html',
})
export class RegisterComponent {
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly showPassword = signal(false);
  protected readonly showConfirm = signal(false);
  protected readonly pwTouched = signal(false);
  protected readonly maxLen = signal(PASSWORD_MAX);
  protected readonly strengthPct = strengthPct;
  protected readonly strengthLabel = strengthLabel;

  protected readonly form = new FormGroup({
    username: new FormControl('', [Validators.required, Validators.minLength(3)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, passwordPolicyValidator]),
    confirmPassword: new FormControl('', [Validators.required]),
  });

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  policy() {
    const { username, email, password } = this.form.value;
    return evaluatePassword(password ?? '', username ?? '', email ?? '');
  }

  ruleItem(passed: boolean, text: string): string {
    return passed ? '✓ ' + text : '✗ ' + text;
  }

  onPasswordInput(): void {
    this.pwTouched.set(true);
  }

  submit(): void {
    this.pwTouched.set(true);
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, email, password, confirmPassword } = this.form.value;
    if (password !== confirmPassword) {
      this.error.set('Passwords do not match.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.register(username ?? '', email ?? '', password ?? '').subscribe((message) => {
      this.loading.set(false);
      if (message) {
        this.error.set(message);
        return;
      }
      const target = this.auth.isAdmin() ? '/admin/dashboard' : '/products';
      this.router.navigate([target]);
    });
  }
}