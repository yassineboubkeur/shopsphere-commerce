import { Component } from '@angular/core';

@Component({
  selector: 'app-loader',
  template: `
    <div class="loader-overlay" aria-label="Loading">
      <div class="spinner"></div>
    </div>
  `,
  styles: `
    .loader-overlay {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 2.5rem;
    }
    .spinner {
      width: 2.25rem;
      height: 2.25rem;
      border: 3px solid var(--border);
      border-top-color: var(--brand);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `,
})
export class Loader {}