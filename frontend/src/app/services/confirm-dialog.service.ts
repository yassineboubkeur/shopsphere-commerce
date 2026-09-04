import { Injectable, signal } from '@angular/core';

export interface ConfirmDialogOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  confirmClass?: 'danger' | 'brand';
  cancelLabel?: string;
  icon?: 'trash' | 'warning' | 'cancel';
}

interface PendingDialog {
  options: Required<Omit<ConfirmDialogOptions, 'confirmClass'> & { confirmClass: 'danger' | 'brand' | '' }>;
  resolve: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly pending = signal<PendingDialog | null>(null);

  open(): PendingDialog | null {
    return this.pending();
  }

  confirm(options: ConfirmDialogOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      const opts = {
        title: options.title ?? 'Are you sure?',
        message: options.message,
        confirmLabel: options.confirmLabel ?? 'Delete',
        cancelLabel: options.cancelLabel ?? 'Cancel',
        confirmClass: options.confirmClass ?? ('danger' as const),
        icon: options.icon ?? ('trash' as const),
      };
      this.pending.set({ options: opts, resolve });
    });
  }

  private finish(result: boolean): void {
    const current = this.pending();
    this.pending.set(null);
    if (current) {
      current.resolve(result);
    }
  }

  confirmAction(): void {
    this.finish(true);
  }

  cancelAction(): void {
    this.finish(false);
  }
}