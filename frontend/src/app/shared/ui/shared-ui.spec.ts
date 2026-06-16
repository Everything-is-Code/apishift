import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoadingSkeletonComponent } from './loading-skeleton.component';
import { StatusBadgeComponent } from './status-badge.component';
import { BusyOverlayComponent } from './busy-overlay.component';

describe('LoadingSkeletonComponent', () => {
  let fixture: ComponentFixture<LoadingSkeletonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingSkeletonComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(LoadingSkeletonComponent);
  });

  it('rendersMessageAndCardSkeletons', () => {
    fixture.componentRef.setInput('message', 'Loading products…');
    fixture.componentRef.setInput('variant', 'cards');
    fixture.componentRef.setInput('cardCount', 3);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Loading products…');
    expect(el.querySelectorAll('.skeleton-card').length).toBe(3);
  });
});

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(StatusBadgeComponent);
  });

  it('appliesVariantClass', () => {
    fixture.componentRef.setInput('label', 'satisfied');
    fixture.componentRef.setInput('variant', 'satisfied');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.badge-satisfied')).toBeTruthy();
  });
});

describe('BusyOverlayComponent', () => {
  let fixture: ComponentFixture<BusyOverlayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BusyOverlayComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(BusyOverlayComponent);
  });

  it('rendersTitleStageAndHint', () => {
    fixture.componentRef.setInput('title', 'Analyzing migration');
    fixture.componentRef.setInput('stage', 'Generating resources…');
    fixture.componentRef.setInput('hint', 'Please wait');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Analyzing migration');
    expect(text).toContain('Generating resources…');
    expect(text).toContain('Please wait');
  });
});
