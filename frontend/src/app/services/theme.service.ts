import { Injectable, Inject, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type Theme = 'light' | 'dark';
export type ThemePreference = Theme | 'system';

const STORAGE_KEY = 'shopsphere-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly preference = signal<ThemePreference>('system');
  readonly resolved = signal<Theme>('light');
  private readonly platformId: object;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.platformId = platformId;
    this.init();
  }

  private init(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const stored = localStorage.getItem(STORAGE_KEY) as ThemePreference | null;
    this.preference.set(stored === 'light' || stored === 'dark' || stored === 'system' ? stored : 'system');

    if (window.matchMedia) {
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (this.preference() === 'system') {
          this.apply(this.compute());
        }
      });
    }

    this.apply(this.compute());
  }

  private compute(): Theme {
    if (this.preference() === 'system') {
      if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) return 'dark';
      return 'light';
    }
    return this.preference() as Theme;
  }

  toggle(): void {
    const next: ThemePreference = this.resolved() === 'dark' ? 'light' : 'dark';
    this.preference.set(next);
    localStorage.setItem(STORAGE_KEY, next);
    this.apply(this.compute());
  }

  set(preference: ThemePreference): void {
    this.preference.set(preference);
    localStorage.setItem(STORAGE_KEY, preference);
    this.apply(this.compute());
  }

  private apply(theme: Theme): void {
    this.resolved.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
  }
}
