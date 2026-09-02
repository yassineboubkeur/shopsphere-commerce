import { TestBed } from '@angular/core/testing';
import { Pagination } from './pagination';

describe('Pagination', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Pagination] }).compileComponents();
  });

  function create(page = 1, totalPages = 1) {
    const fixture = TestBed.createComponent(Pagination);
    const component = fixture.componentInstance;
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('totalPages', totalPages);
    fixture.detectChanges();
    return { fixture, component };
  }

  it('should create', () => {
    const { component } = create();
    expect(component).toBeTruthy();
  });

  it('should not render when there is a single page', () => {
    const { fixture } = create(1, 1);
    expect((fixture.nativeElement as HTMLElement).querySelector('.pagination')).toBeNull();
  });

  it('should show current page and total pages', () => {
    const { fixture } = create(2, 5);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Page 2 of 5');
  });

  it('should disable Prev on the first page', () => {
    const { fixture } = create(1, 5);
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button');
    expect(buttons[0].disabled).toBe(true);
    expect(buttons[1].disabled).toBe(false);
  });

  it('should disable Next on the last page', () => {
    const { fixture } = create(5, 5);
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button');
    expect(buttons[0].disabled).toBe(false);
    expect(buttons[1].disabled).toBe(true);
  });

  it('should emit page + 1 when Next is clicked', () => {
    const { fixture, component } = create(2, 5);
    let value = 0;
    component.pageChange.subscribe((p) => (value = p));
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button');
    buttons[1].click();
    expect(value).toBe(3);
  });

  it('should emit page - 1 when Prev is clicked', () => {
    const { fixture, component } = create(3, 5);
    let value = 0;
    component.pageChange.subscribe((p) => (value = p));
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button');
    buttons[0].click();
    expect(value).toBe(2);
  });
});
