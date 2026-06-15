import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MigrationWizardComponent } from './migration-wizard.component';
import { ApiService, MigrationPlan, MigrationPrerequisite } from '../../services/api.service';

describe('MigrationWizardComponent prerequisites', () => {
  let fixture: ComponentFixture<MigrationWizardComponent>;
  let component: MigrationWizardComponent;

  const prerequisites: MigrationPrerequisite[] = [
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
    {
      id: 'rhcl-core',
      category: 'core-policy',
      title: 'RHCL core',
      description: 'Kuadrant core',
      requiredByPlan: true,
      optionalTier: false,
      status: 'satisfied',
      triggeredByCount: 1,
    },
    {
      id: 'gateforge-cluster-api',
      category: 'tool-config',
      title: 'Kubernetes API',
      description: 'Cluster connection',
      requiredByPlan: true,
      optionalTier: false,
      status: 'missing',
      triggeredByCount: 0,
    },
  ];

  const mockPlan: MigrationPlan = {
    id: 'plan-1',
    gatewayStrategy: 'shared',
    sourceProducts: ['demo-api'],
    resources: [{ kind: 'Gateway', name: 'gw', namespace: 'ns', yaml: 'kind: Gateway' }],
    aiAnalysis: 'ok',
    createdAt: new Date().toISOString(),
    prerequisites,
    targetClusterId: 'local',
    targetClusterLabel: 'Local',
  };

  beforeEach(async () => {
    const apiMock = {
      getProducts: () => of([]),
      getFeatures: () => of({ developerHub: { enabled: false, url: '' } }),
      getTargetClusters: () => of([{
        id: 'local',
        label: 'Local',
        apiServerUrl: '',
        token: '',
        authType: 'in-cluster',
        verifySsl: true,
        enabled: true,
      }]),
    } as Partial<ApiService>;

    await TestBed.configureTestingModule({
      imports: [MigrationWizardComponent],
      providers: [
        { provide: ApiService, useValue: apiMock },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MigrationWizardComponent);
    component = fixture.componentInstance;
    component.step = 3;
    component.plan = mockPlan;
    fixture.detectChanges();
  });

  it('shows prerequisites panel with apply-vs-analyze banner', () => {
    const el: HTMLElement = fixture.nativeElement;
    const panel = el.querySelector('.prerequisites-panel');
    expect(panel).toBeTruthy();
    expect(el.textContent).toContain('Required for apply, not for analysis');
  });

  it('groups prerequisites by category in defined order', () => {
    const sections = component.prerequisiteSections;
    expect(sections.length).toBe(3);
    expect(sections[0].category).toBe('connectivity');
    expect(sections[0].label).toBe('Connectivity');
    expect(sections[1].category).toBe('core-policy');
    expect(sections[2].category).toBe('tool-config');
  });

  it('renders status badge classes for satisfied, missing, and unknown', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.badge-unknown')).toBeTruthy();
    expect(el.querySelector('.badge-satisfied')).toBeTruthy();
    expect(el.querySelector('.badge-missing')).toBeTruthy();
  });

  it('hides panel when prerequisites are empty', () => {
    component.plan = { ...mockPlan, prerequisites: [] };
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.prerequisites-panel')).toBeFalsy();
  });

  it('toggles collapsible panel and aria-expanded', () => {
    component.prerequisitesOpen = true;
    fixture.detectChanges();
    const header = fixture.nativeElement.querySelector('.prerequisites-banner');
    expect(header.getAttribute('aria-expanded')).toBe('true');

    component.togglePrerequisites();
    fixture.detectChanges();
    expect(component.prerequisitesOpen).toBe(false);
    expect(header.getAttribute('aria-expanded')).toBe('false');
  });
});
