import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WizardStepNavComponent } from './wizard-step-nav.component';

describe('WizardStepNavComponent', () => {
  let fixture: ComponentFixture<WizardStepNavComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WizardStepNavComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WizardStepNavComponent);
    fixture.componentRef.setInput('step', 2);
    fixture.componentRef.setInput('stepLabels', ['Products', 'Strategy', 'Review']);
  });

  it('rendersStepNodesAndHighlightsActive', () => {
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Products');
    expect(el.textContent).toContain('Strategy');
    expect(el.querySelectorAll('.step-node.active').length).toBe(1);
    expect(el.querySelectorAll('.step-node.done').length).toBe(1);
  });
});
