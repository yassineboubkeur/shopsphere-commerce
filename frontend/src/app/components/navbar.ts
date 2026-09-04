import { Component, DestroyRef, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { interval, of } from 'rxjs';
import { catchError, filter, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { ThemeService } from '../services/theme.service';
import { NotificationService } from '../services/notification.service';
import { Notification } from '../models/models';

const POLL_MS = 10_000;
const READ_STATUSES = ['SEEN', 'READ', 'PROCESSED'];

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  styleUrl: './navbar.css',
  templateUrl: './navbar.html',
})
export class Navbar {
  private readonly auth = inject(AuthService);
  protected readonly user = this.auth.user;
  protected readonly isLoggedIn = this.auth.isLoggedIn;
  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly cartCount = inject(CartService).count;

  private readonly notificationsService = inject(NotificationService);
  private readonly unread = signal(0);
  protected readonly notificationCount = this.unread.asReadonly();
  protected readonly notifications = signal<Notification[]>([]);
  protected readonly panelOpen = signal(false);

  private readonly router = inject(Router);
  protected readonly menuOpen = signal(false);
  private readonly elementRef = inject(ElementRef);
  protected readonly themeService = inject(ThemeService);
  protected readonly isDark = this.themeService.resolved.asReadonly();

  constructor(private readonly destroyRef: DestroyRef) {
    const authUserId = this.auth.userId;
    interval(POLL_MS)
      .pipe(
        map(() => authUserId()),
        filter((id): id is number => id !== null),
        switchMap((id) =>
          this.notificationsService.getByUser(id).pipe(catchError(() => of([]))),
        ),
      )
      .subscribe((list) => this.apply(list));

    this.destroyRef.onDestroy(() => {
      this.unread.set(0);
      this.notifications.set([]);
      this.panelOpen.set(false);
    });
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.closeNotifications();
  }

  onPanelClick(event: Event): void {
    event.stopPropagation();
  }

  toggleNotifications(): void {
    const next = !this.panelOpen();
    this.panelOpen.set(next);
    if (next) this.refresh();
    this.closeMenu();
  }

  closeNotifications(): void {
    if (this.panelOpen()) this.panelOpen.set(false);
  }

  private refresh(): void {
    const authUserId = this.auth.userId;
    const id = authUserId();
    if (id === null) return;
    this.notificationsService.getByUser(id).subscribe((list) => this.apply(list));
  }

  private apply(list: Notification[]): void {
    this.notifications.set([...list].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')));
    const unread = list.filter((n) => !READ_STATUSES.includes((n.status || '').toUpperCase())).length;
    this.unread.set(unread);
  }

  isRead(n: Notification): boolean {
    return READ_STATUSES.includes((n.status || '').toUpperCase());
  }

  timeLabel(value: string | undefined): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const now = Date.now();
    const diff = now - date.getTime();
    const minutes = Math.floor(diff / 60_000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days} d ago`;
    return date.toLocaleDateString();
  }

  markNotifAsRead(n: Notification): void {
    if (this.isRead(n)) return;
    this.notificationsService.markAsRead(n.id).subscribe({
      next: () => {
        this.notifications.update((list) =>
          list.map((item) => (item.id === n.id ? { ...item, status: 'SEEN' } : item)),
        );
        this.unread.update((c) => Math.max(0, c - 1));
      },
      error: () => {},
    });
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  toggleMenu(): void {
    this.menuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.closeNotifications();
    this.closeMenu();
    this.auth.logout();
    this.router.navigate(['/products']);
  }
}