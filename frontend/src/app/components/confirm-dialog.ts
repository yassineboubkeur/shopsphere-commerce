import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ConfirmDialogService } from '../services/confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  imports: [],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  private readonly service = inject(ConfirmDialogService);

  protected readonly dialog = () => this.service.open();

  protected get title(): string {
    return this.dialog()?.options.title ?? '';
  }

  protected get message(): string {
    return this.dialog()?.options.message ?? '';
  }

  protected get confirmLabel(): string {
    return this.dialog()?.options.confirmLabel ?? 'Delete';
  }

  protected get cancelLabel(): string {
    return this.dialog()?.options.cancelLabel ?? 'Cancel';
  }

  protected get confirmClass(): string {
    return this.dialog()?.options.confirmClass || 'danger';
  }

  protected get icon(): string {
    return this.dialog()?.options.icon ?? 'trash';
  }

  confirmAction(): void {
    this.service.confirmAction();
  }

  cancelAction(): void {
    this.service.cancelAction();
  }
}