import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  template: `
    @if (totalPages() > 1) {
      <nav class="pagination">
        <button type="button" [disabled]="page() <= 1" (click)="change(page() - 1)">Prev</button>
        <span>Page {{ page() }} of {{ totalPages() }}</span>
        <button type="button" [disabled]="page() >= totalPages()" (click)="change(page() + 1)">Next</button>
      </nav>
    }
  `,
  styles: `
    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      margin: 1.5rem 0;
    }
    button {
      border: 1px solid var(--border-strong);
      background: var(--surface);
      color: var(--text);
      padding: 0.4rem 1rem;
      border-radius: 0.35rem;
      cursor: pointer;
      font-size: 0.9rem;
    }
    button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    span {
      font-size: 0.9rem;
      color: var(--text-muted);
    }
  `,
})
export class Pagination {
  readonly page = input(1);
  readonly totalPages = input(1);
  readonly pageChange = output<number>();

  change(p: number): void {
    this.pageChange.emit(p);
  }
}