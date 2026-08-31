import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, AuthUser } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<AuthUser | null>(this.readStoredUser());
  readonly user = this.userSignal.asReadonly();
  readonly isLoggedIn = computed(() => !!this.userSignal());
  readonly isAdmin = computed(() => this.userSignal()?.role?.includes('ADMIN') ?? false);
  readonly userId = computed(() => this.userSignal()?.id ?? null);

  constructor(private readonly http: HttpClient) {}

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getProfile(): Observable<AuthUser | null> {
    return this.http
      .get<{ id: number; username: string; email: string; role: string }>(
        `${environment.apiUrl}/api/user/profile`,
      )
      .pipe(
        map((u) => {
          const user: AuthUser = {
            id: u.id,
            username: u.username,
            email: u.email,
            role: this.normalizeRole(u.role),
          };
          localStorage.setItem('user', JSON.stringify(user));
          this.userSignal.set(user);
          return user;
        }),
        catchError(() => of(null)),
      );
  }

  login(email: string, password: string): Observable<string | null> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, { email, password })
      .pipe(
        tap((res) => this.saveSession(res)),
        map(() => null),
        catchError((err) => of(this.errorMessage(err))),
      );
  }

  register(username: string, email: string, password: string): Observable<string | null> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/register`, { username, email, password })
      .pipe(
        tap((res) => this.saveSession(res)),
        map(() => null),
        catchError((err) => of(this.registerErrorMessage(err))),
      );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.userSignal.set(null);
  }

  private saveSession(res: AuthResponse): void {
    localStorage.setItem('token', res.token);
    const user: AuthUser = {
      id: res.id,
      username: res.username,
      email: res.email,
      role: this.normalizeRole(res.role),
    };
    localStorage.setItem('user', JSON.stringify(user));
    this.userSignal.set(user);
  }

  private readStoredUser(): AuthUser | null {
    try {
      const raw = localStorage.getItem('user');
      if (!raw) return null;
      const parsed = JSON.parse(raw) as AuthUser;
      parsed.role = this.normalizeRole(parsed.role);
      return parsed;
    } catch {
      return null;
    }
  }

  private normalizeRole(role: string | undefined | null): string {
    const r = (role ?? '').trim().toUpperCase();
    if (r.includes('ADMIN')) return 'ADMIN';
    if (r.includes('USER')) return 'USER';
    return r || 'USER';
  }

  private errorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'status' in err) {
      const status = (err as { status: number }).status;
      if (status === 401) return 'Invalid email or password.';
      if (status === 0) return 'Cannot reach the server. Is the backend running?';
    }
    return 'Login failed. Please try again.';
  }

  private registerErrorMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'status' in err) {
      const status = (err as { status: number }).status;
      if (status === 400) return 'Registration failed. Please check your details.';
      if (status === 409) return 'This email is already registered.';
      if (status === 0) return 'Cannot reach the server. Is the backend running?';
    }
    return 'Registration failed. Please try again.';
  }
}