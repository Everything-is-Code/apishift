import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MigrationWizardComponent } from './migration-wizard.component';
import { MigrationWizardStateService } from './migration-wizard.state.service';
import { ClusterApiService } from '../../core/api/cluster-api.service';
import { MigrationApiService } from '../../core/api/migration-api.service';
import { ThreeScaleApiService } from '../../core/api/threescale-api.service';
import {
  MigrationPlan,
  MigrationPrerequisite,
  ThreeScaleProduct,
} from '../../core/api/models';

const mockProduct: ThreeScaleProduct = {
  name: 'demo-api',
  namespace: 'default',
  systemName: 'demo-api',
  description: 'Demo API',
  deploymentOption: 'hosted',
  mappingRules: [],
  backendUsages: [{ backendName: 'api', path: '/' }],
  authentication: {},
  source: 'local',
};

const otherProduct: ThreeScaleProduct = {
  ...mockProduct,
  name: 'other-api',
  systemName: 'other-api',
};

const localCluster = {
  id: 'local',
  label: 'Local',
  apiServerUrl: '',
  authType: 'in-cluster',
  verifySsl: true,
  enabled: true,
  credentialConfigured: false,
};

interface WizardApiSpies {
  cluster: jasmine.SpyObj<ClusterApiService>;
  migration: jasmine.SpyObj<MigrationApiService>;
  threescale: jasmine.SpyObj<ThreeScaleApiService>;
}

function createApiSpies(): WizardApiSpies {
  return {
    threescale: jasmine.createSpyObj<ThreeScaleApiService>('ThreeScaleApiService', ['getProducts']),
    cluster: jasmine.createSpyObj<ClusterApiService>('ClusterApiService', [
      'getFeatures',
      'getTargetClusters',
      'getClusterReadiness',
    ]),
    migration: jasmine.createSpyObj<MigrationApiService>('MigrationApiService', ['analyze', 'importExport']),
  };
}

function stubApiDefaults(spies: WizardApiSpies): void {
  spies.threescale.getProducts.and.returnValue(of([]));
  spies.cluster.getFeatures.and.returnValue(of({ developerHub: { enabled: false, url: '' } }));
  spies.cluster.getTargetClusters.and.returnValue(of([localCluster]));
}

async function configureWizard(spies: WizardApiSpies): Promise<{
  fixture: ComponentFixture<MigrationWizardComponent>;
  wizard: MigrationWizardStateService;
}> {
  await TestBed.configureTestingModule({
    imports: [MigrationWizardComponent],
    providers: [
      { provide: ThreeScaleApiService, useValue: spies.threescale },
      { provide: ClusterApiService, useValue: spies.cluster },
      { provide: MigrationApiService, useValue: spies.migration },
      provideRouter([]),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(MigrationWizardComponent);
  return { fixture, wizard: fixture.componentInstance.wizard };
}

describe('MigrationWizardComponent init', () => {
  let spies: WizardApiSpies;

  beforeEach(() => {
    spies = createApiSpies();
    stubApiDefaults(spies);
  });

  it('ngOnInit_loadsProducts', async () => {
    spies.threescale.getProducts.and.returnValue(of([mockProduct, otherProduct]));
    const { fixture, wizard } = await configureWizard(spies);

    fixture.detectChanges();

    expect(wizard.products.length).toBe(2);
    expect(wizard.products.every(p => !p.selected)).toBe(true);
    expect(wizard.productsLoading).toBe(false);
  });

  it('ngOnInit_productError_clearsLoading', async () => {
    spies.threescale.getProducts.and.returnValue(throwError(() => new Error('load failed')));
    const { fixture, wizard } = await configureWizard(spies);

    fixture.detectChanges();

    expect(wizard.productsLoading).toBe(false);
    expect(wizard.products.length).toBe(0);
  });

  it('ngOnInit_clusterError_fallsBackToLocal', async () => {
    spies.cluster.getTargetClusters.and.returnValue(throwError(() => new Error('cluster failed')));
    const { fixture, wizard } = await configureWizard(spies);

    fixture.detectChanges();

    expect(wizard.targetClusters.length).toBe(1);
    expect(wizard.targetClusters[0].id).toBe('local');
  });
});

describe('MigrationWizardComponent step 1', () => {
  let wizard: MigrationWizardStateService;

  beforeEach(async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    spies.threescale.getProducts.and.returnValue(of([mockProduct, otherProduct]));
    const configured = await configureWizard(spies);
    configured.fixture.detectChanges();
    wizard = configured.wizard;
  });

  it('selectedCount_reflectsSelection', () => {
    wizard.products[0].selected = true;
    expect(wizard.selectedCount).toBe(1);
  });

  it('visibleProducts_filtersByQuery', () => {
    wizard.productSearchQuery = 'other';
    expect(wizard.visibleProducts.length).toBe(1);
    expect(wizard.visibleProducts[0].product.name).toBe('other-api');
  });

  it('selectAllFiltered_togglesVisible', () => {
    wizard.productSearchQuery = 'demo';
    wizard.selectAllFiltered();
    expect(wizard.visibleProducts.every(p => p.selected)).toBe(true);
    wizard.selectAllFiltered();
    expect(wizard.visibleProducts.every(p => !p.selected)).toBe(true);
  });
});

describe('MigrationWizardComponent export import', () => {
  const exportProduct: ThreeScaleProduct = {
    ...mockProduct,
    name: 'Seed Alpha',
    systemName: 'seed_alpha',
    source: 'export-v1 (https://example.com)',
  };

  it('setProductSource_export_clearsLiveProducts', async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    spies.threescale.getProducts.and.returnValue(of([mockProduct]));
    const { fixture, wizard } = await configureWizard(spies);
    fixture.detectChanges();

    wizard.setProductSource('export');

    expect(wizard.productSource).toBe('export');
    expect(wizard.products.length).toBe(0);
    expect(wizard.productsLoading).toBe(false);
  });

  it('importExportArchive_successReloadsProducts', async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    spies.migration.importExport.and.returnValue(of({
      importMode: 'export-v1',
      productCount: 1,
      products: [{ name: 'Seed Alpha', systemName: 'seed_alpha', serviceId: 1 }],
      manifest: { schemaVersion: '1.0', adminUrl: 'https://example.com', exportedAt: '2024-01-01' },
    }));
    spies.threescale.getProducts.and.returnValue(of([exportProduct]));
    const { fixture, wizard } = await configureWizard(spies);
    fixture.detectChanges();

    wizard.setProductSource('export');
    wizard.selectedExportFile = new File(['zip'], 'export-minimal.zip', { type: 'application/zip' });
    wizard.importExportArchive();
    fixture.detectChanges();

    expect(spies.migration.importExport).toHaveBeenCalled();
    expect(spies.threescale.getProducts).toHaveBeenCalled();
    expect(wizard.products.length).toBe(1);
    expect(wizard.importResult?.productCount).toBe(1);
    expect(wizard.importing).toBe(false);
    expect(wizard.isExportProductFn(wizard.products[0].product)).toBe(true);
  });

  it('importExportArchive_errorShowsMessage', async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    spies.migration.importExport.and.returnValue(throwError(() => ({ error: 'Only .zip export archives are supported' })));
    const { fixture, wizard } = await configureWizard(spies);
    fixture.detectChanges();

    wizard.setProductSource('export');
    wizard.selectedExportFile = new File(['bad'], 'archive.tar.gz', { type: 'application/gzip' });
    wizard.importExportArchive();

    expect(wizard.importError).toContain('.zip');
    expect(spies.migration.importExport).not.toHaveBeenCalled();
  });
});

describe('MigrationWizardComponent analyze', () => {
  const mockPlan: MigrationPlan = {
    id: 'plan-1',
    gatewayStrategy: 'dual',
    sourceProducts: ['demo-api'],
    resources: [{ kind: 'Gateway', name: 'gw', namespace: 'ns', yaml: 'kind: Gateway' }],
    aiAnalysis: 'ok',
    createdAt: new Date().toISOString(),
    targetClusterId: 'local',
    targetClusterLabel: 'Local',
  };

  it('analyze_advancesToStep3', async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    spies.threescale.getProducts.and.returnValue(of([mockProduct]));
    spies.migration.analyze.and.returnValue(of(mockPlan));
    const { fixture, wizard } = await configureWizard(spies);
    fixture.detectChanges();

    wizard.products[0].selected = true;
    wizard.step = 2;
    wizard.gatewayStrategy = 'dual';
    wizard.analyze();

    expect(spies.migration.analyze).toHaveBeenCalledWith('dual', ['demo-api'], 'local');
    expect(wizard.step).toBe(3);
    expect(wizard.plan).toEqual(mockPlan);
    expect(wizard.analyzing).toBe(false);
  });

  it('analyze_skipsWhenAlreadyAnalyzing', async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    spies.migration.analyze.and.returnValue(of(mockPlan));
    const { wizard } = await configureWizard(spies);

    wizard.analyzing = true;
    wizard.analyze();

    expect(spies.migration.analyze).not.toHaveBeenCalled();
  });
});

describe('MigrationWizardComponent review helpers', () => {
  let wizard: MigrationWizardStateService;

  beforeEach(async () => {
    const spies = createApiSpies();
    stubApiDefaults(spies);
    const configured = await configureWizard(spies);
    wizard = configured.wizard;
  });

  it('hasConsumerApiKeySecrets_trueWhenSecretPresent', () => {
    wizard.plan = {
      id: 'p',
      gatewayStrategy: 'shared',
      sourceProducts: [],
      resources: [{ kind: 'Secret', name: 's', namespace: 'ns', yaml: 'kind: Secret' }],
      aiAnalysis: '',
      createdAt: '',
    };
    expect(wizard.hasConsumerApiKeySecrets).toBe(true);
    expect(wizard.consumerApiKeySecretCount).toBe(1);
  });

  it('hasOidcJwtAuth_trueWhenIssuerInYaml', () => {
    wizard.plan = {
      id: 'p',
      gatewayStrategy: 'shared',
      sourceProducts: [],
      resources: [{
        kind: 'AuthPolicy',
        name: 'auth',
        namespace: 'ns',
        yaml: 'issuerUrl: https://sso.example.com',
      }],
      aiAnalysis: '',
      createdAt: '',
    };
    expect(wizard.hasOidcJwtAuth).toBe(true);
  });
});

describe('MigrationWizardComponent prerequisites', () => {
  let fixture: ComponentFixture<MigrationWizardComponent>;
  let wizard: MigrationWizardStateService;

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
    const spies = createApiSpies();
    stubApiDefaults(spies);

    await TestBed.configureTestingModule({
      imports: [MigrationWizardComponent],
      providers: [
        { provide: ThreeScaleApiService, useValue: spies.threescale },
        { provide: ClusterApiService, useValue: spies.cluster },
        { provide: MigrationApiService, useValue: spies.migration },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MigrationWizardComponent);
    wizard = fixture.componentInstance.wizard;
    wizard.step = 3;
    wizard.plan = mockPlan;
    fixture.detectChanges();
  });

  it('shows prerequisites panel with apply-vs-analyze banner', () => {
    const el: HTMLElement = fixture.nativeElement;
    const panel = el.querySelector('.prerequisites-panel');
    expect(panel).toBeTruthy();
    expect(el.textContent).toContain('Required for apply, not for analysis');
  });

  it('groups prerequisites by category in defined order', () => {
    const sections = wizard.prerequisiteSections;
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
    wizard.plan = { ...mockPlan, prerequisites: [] };
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.prerequisites-panel')).toBeFalsy();
  });

  it('toggles collapsible panel and aria-expanded', () => {
    wizard.prerequisitesOpen = true;
    fixture.detectChanges();
    const header = fixture.nativeElement.querySelector('.prerequisites-banner');
    expect(header.getAttribute('aria-expanded')).toBe('true');

    wizard.togglePrerequisites();
    fixture.detectChanges();
    expect(wizard.prerequisitesOpen).toBe(false);
    expect(header.getAttribute('aria-expanded')).toBe('false');
  });
});
