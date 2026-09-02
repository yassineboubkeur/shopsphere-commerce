import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    window.localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  it('should default to the system/light theme', () => {
    expect(service.resolved()).toBe('light');
  });

  it('should toggle between light and dark', () => {
    service.toggle();
    expect(service.resolved()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    service.toggle();
    expect(service.resolved()).toBe('light');
  });

  it('should persist the preference to localStorage', () => {
    service.toggle();
    expect(window.localStorage.getItem('shopsphere-theme')).toBe('dark');
  });

  it('should apply the requested theme', () => {
    service.set('dark');
    expect(service.resolved()).toBe('dark');
    service.set('light');
    expect(service.resolved()).toBe('light');
  });

  it('should restore the stored preference on construction', () => {
    window.localStorage.setItem('shopsphere-theme', 'dark');
    TestBed.resetTestingModule();
    service = TestBed.inject(ThemeService);
    expect(service.resolved()).toBe('dark');
  });
});
