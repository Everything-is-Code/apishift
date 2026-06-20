import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WizardStepReviewComponent } from './wizard-step-review.component';
import { WizardReviewActions, WizardReviewState } from './wizard-review.types';
import { MigrationPlan } from '../../../core/api/models';

describe('WizardStepReviewComponent', () => {
  let fixture: ComponentFixture<WizardStepReviewComponent>;
  let actions: jasmine.SpyObj<WizardReviewActions>;

  const plan: MigrationPlan = {
    id: 'plan-1',
    gatewayStrategy: 'shared',
    sourceProducts: ['demo-api'],
    resources: [{ kind: 'Gateway', name: 'gw', namespace: 'ns', yaml: 'kind: Gateway' }],
    aiAnalysis: 'Looks good',
    createdAt: '',
    prerequisites: [
      {
        id: 'gateway-api',
        category: 'connectivity',
        title: 'Gateway API',
        description: 'Gateway API CRDs',
        requiredByPlan: true,
        optionalTier: false,
        status: 'unknown',
        triggeredByCount: 1,
      },
    ],
  };

  const state: WizardReviewState = {
    plan,
    prerequisitesOpen: true,
    aiAnalysisOpen: true,
    readinessLoading: false,
    prerequisiteSections: [],
    consumerApiKeySecretCount: 0,
    hasOidcJwtAuth: false,
    hasConsumerApiKeySecrets: false,
    yamlOpen: {},
    editMode: {},
    editedYamls: {},
    resourceEnabled: {},
    applying: false,
    applyResult: null,
    reverting: false,
    revertResult: null,
    developerHubEnabled: false,
    developerHubUrl: '',
    componentEditMode: false,
    editedComponentYaml: '',
    catalogCopied: false,
    registering: false,
    registrationDone: false,
    registrationError: '',
    testCommands: [],
  };

  beforeEach(async () => {
    actions = jasmine.createSpyObj<WizardReviewActions>('WizardReviewActions', [
      'togglePrerequisites',
      'refreshReadiness',
      'toggleAiAnalysis',
      'applyMigration',
      'goBack',
    ]);

    await TestBed.configureTestingModule({
      imports: [WizardStepReviewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WizardStepReviewComponent);
    fixture.componentRef.setInput('s', state);
    fixture.componentRef.setInput('a', actions);
  });

  it('rendersPlanSummaryAndActionButtons', () => {
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Looks good');
    expect(el.textContent).toContain('Gateway');

    el.querySelector('.prerequisites-banner')?.dispatchEvent(new Event('click'));
    expect(actions.togglePrerequisites).toHaveBeenCalled();

    el.querySelector('.btn-refresh-readiness')?.dispatchEvent(new Event('click'));
    expect(actions.refreshReadiness).toHaveBeenCalled();

    el.querySelector('.btn-secondary')?.dispatchEvent(new Event('click'));
    expect(actions.goBack).toHaveBeenCalled();

    el.querySelector('.btn-apply')?.dispatchEvent(new Event('click'));
    expect(actions.applyMigration).toHaveBeenCalled();
  });
});
