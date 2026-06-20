import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WizardHistoryComponent } from './wizard-history.component';
import { WizardHistoryActions, WizardHistoryState } from './wizard-review.types';
import { MigrationPlan } from '../../../core/api/models';

describe('WizardHistoryComponent', () => {
  let fixture: ComponentFixture<WizardHistoryComponent>;
  let actions: jasmine.SpyObj<WizardHistoryActions>;

  const plan: MigrationPlan = {
    id: 'plan-1',
    gatewayStrategy: 'shared',
    sourceProducts: ['demo-api'],
    resources: [],
    aiAnalysis: '',
    createdAt: '',
  };

  const state: WizardHistoryState = {
    historyOpen: true,
    historyLoading: false,
    allPlans: [plan],
    planSelection: { 'plan-1': true },
    deleteGateway: false,
    bulkReverting: false,
    bulkResult: null,
    driftResults: {},
    driftLoading: {},
    allSelected: true,
    selectedPlanIds: ['plan-1'],
  };

  beforeEach(async () => {
    actions = jasmine.createSpyObj<WizardHistoryActions>('WizardHistoryActions', [
      'toggleHistory',
      'toggleSelectAll',
      'togglePlanSelection',
      'confirmBulkRevert',
      'checkDrift',
      'setDeleteGateway',
    ]);

    await TestBed.configureTestingModule({
      imports: [WizardHistoryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WizardHistoryComponent);
    fixture.componentRef.setInput('s', state);
    fixture.componentRef.setInput('a', actions);
  });

  it('rendersPlansAndToolbarActions', () => {
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('plan-1');
    expect(el.textContent).toContain('Revert to 3scale (1)');

    el.querySelector('.history-header')?.dispatchEvent(new Event('click'));
    expect(actions.toggleHistory).toHaveBeenCalled();

    (el.querySelector('.history-toolbar input[type="checkbox"]') as HTMLInputElement).dispatchEvent(
      new Event('change'),
    );
    expect(actions.toggleSelectAll).toHaveBeenCalled();

    el.querySelector('.btn-bulk-revert')?.dispatchEvent(new Event('click'));
    expect(actions.confirmBulkRevert).toHaveBeenCalled();

    el.querySelector('.btn-drift')?.dispatchEvent(new Event('click'));
    expect(actions.checkDrift).toHaveBeenCalledWith('plan-1');
  });
});
