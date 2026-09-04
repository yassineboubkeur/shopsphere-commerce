import { Component, computed, inject, signal } from '@angular/core';
import { AdminService, AdminUserRow } from '../services/admin.service';
import { AuthService } from '../services/auth.service';
import { ConfirmDialogService } from '../services/confirm-dialog.service';

@Component({
  selector: 'app-user-management',
  styleUrl: './user-management.css',
  templateUrl: './user-management.html',
})
export class UserManagementComponent {
  private readonly admin = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly confirm = inject(ConfirmDialogService);

  protected readonly users = signal<AdminUserRow[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly workingId = signal<number | null>(null);
  protected readonly toast = signal('');
  protected readonly filter = signal('ALL');

  protected readonly selfId = this.auth.userId();

  protected readonly visible = computed(() => {
    const f = this.filter();
    if (f === 'ALL') return this.users();
    return this.users().filter((u) => u.role === f);
  });

  protected readonly adminCount = computed(() => this.users().filter((u) => u.role === 'ADMIN').length);
  protected readonly userCount = computed(() => this.users().filter((u) => u.role === 'USER').length);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.admin.getUsers().subscribe({
      next: (u) => {
        this.users.set(u);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  toggleRole(u: AdminUserRow): void {
    if (u.id === this.selfId) {
      this.toast.set('You cannot change your own role.');
      setTimeout(() => this.toast.set(''), 4000);
      return;
    }
    const target = u.role === 'ADMIN' ? 'USER' : 'ADMIN';
    this.workingId.set(u.id);
    this.toast.set('');
    this.admin.updateRole(u.id, target).subscribe({
      next: () => {
        this.workingId.set(null);
        this.toast.set(`${u.email} is now ${target}.`);
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.workingId.set(null);
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  async remove(u: AdminUserRow): Promise<void> {
    if (u.id === this.selfId) {
      this.toast.set('You cannot delete your own account.');
      setTimeout(() => this.toast.set(''), 4000);
      return;
    }
    const ok = await this.confirm.confirm({
      title: 'Delete this user?',
      message: `Delete user ${u.email}? This cannot be undone.`,
      confirmLabel: 'Delete',
    });
    if (!ok) return;
    this.workingId.set(u.id);
    this.toast.set('');
    this.admin.deleteUser(u.id).subscribe({
      next: () => {
        this.workingId.set(null);
        this.toast.set(`${u.email} deleted.`);
        setTimeout(() => this.toast.set(''), 3000);
        this.load();
      },
      error: (err: unknown) => {
        this.workingId.set(null);
        this.toast.set(this.httpError(err));
        setTimeout(() => this.toast.set(''), 4000);
      },
    });
  }

  date(u: AdminUserRow): string {
    if (!u.createdAt) return '—';
    const d = new Date(u.createdAt);
    return isNaN(d.getTime()) ? '—' : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  }

  setFilter(f: string): void {
    this.filter.set(f);
  }

  isSelf(u: AdminUserRow): boolean {
    return u.id === this.selfId;
  }

  initial(name: string): string {
    return (name || '?').trim().charAt(0).toUpperCase();
  }

  httpError(err: unknown): string {
    const anyErr = err as { error?: { message?: string; error?: string } };
    return anyErr?.error?.message || anyErr?.error?.error || 'Request failed.';
  }
}