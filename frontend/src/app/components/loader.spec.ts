import { TestBed } from '@angular/core/testing';
import { Loader } from './loader';

describe('Loader', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Loader] }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(Loader);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the spinner with a loading label', () => {
    const fixture = TestBed.createComponent(Loader);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.spinner')).toBeTruthy();
    expect(el.querySelector('.loader-overlay')?.getAttribute('aria-label')).toBe('Loading');
  });
});
