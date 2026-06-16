import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MigrationWizardComponent } from './migration-wizard.component';
import {
  ApiService,
  MigrationPlan,
  MigrationPrerequisite,
  ThreeScaleProduct,
} from '../../core/api/api.service';

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
  token: '',
  authType: 'in-cluster',
  verifySsl: true,
  enabled: true,
};

function createApiSpy(): jasmine.SpyObj<ApiService> {
  return jasmine.createSpyObj<ApiService>('ApiService', [
    'getProducts',
    'getFeatures',
    'getTargetClusters',
    'analyzeMigration',
    'getClusterReadiness',
    'importExport',
  ]);
}

function stubApiDefaults(apiSpy: jasmine.SpyObj<ApiService>): void {
  apiSpy.getProducts.and.returnValue(of([]));
  apiSpy.getFeatures.and.returnValue(of({ developerHub: { enabled: false, url: '' } }));
  apiSpy.getTargetClusters.and.returnValue(of([localCluster]));
}

async function configureWizard(apiSpy: jasmine.SpyObj<ApiService>): Promise<{
  fixture: ComponentFixture<MigrationWizardComponent>;
  component: MigrationWizardComponent;
}> {
  await TestBed.configureTestingModule({
    imports: [MigrationWizardComponent],
    providers: [
      { provide: ApiService, useValue: apiSpy },
      provideRouter([]),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(MigrationWizardComponent);
  return { fixture, component: fixture.componentInstance };
}

describe('MigrationWizardComponent init', () => {
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
  });

  it('ngOnInit_loadsProducts', async () => {
    apiSpy.getProducts.and.returnValue(of([mockProduct, otherProduct]));
    const { fixture, component } = await configureWizard(apiSpy);

    fixture.detectChanges();

    expect(component.products.length).toBe(2);
    expect(component.products.every(p => !p.selected)).toBe(true);
    expect(component.productsLoading).toBe(false);
  });

  it('ngOnInit_productError_clearsLoading', async () => {
    apiSpy.getProducts.and.returnValue(throwError(() => new Error('load failed')));
    const { fixture, component } = await configureWizard(apiSpy);

    fixture.detectChanges();

    expect(component.productsLoading).toBe(false);
    expect(component.products.length).toBe(0);
  });

  it('ngOnInit_clusterError_fallsBackToLocal', async () => {
    apiSpy.getTargetClusters.and.returnValue(throwError(() => new Error('cluster failed')));
    const { fixture, component } = await configureWizard(apiSpy);

    fixture.detectChanges();

    expect(component.targetClusters.length).toBe(1);
    expect(component.targetClusters[0].id).toBe('local');
  });
});

describe('MigrationWizardComponent step 1', () => {
  let component: MigrationWizardComponent;

  beforeEach(async () => {
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    apiSpy.getProducts.and.returnValue(of([mockProduct, otherProduct]));
    const configured = await configureWizard(apiSpy);
    configured.fixture.detectChanges();
    component = configured.component;
  });

  it('selectedCount_reflectsSelection', () => {
    component.products[0].selected = true;
    expect(component.selectedCount).toBe(1);
  });

  it('visibleProducts_filtersByQuery', () => {
    component.productSearchQuery = 'other';
    expect(component.visibleProducts.length).toBe(1);
    expect(component.visibleProducts[0].product.name).toBe('other-api');
  });

  it('selectAllFiltered_togglesVisible', () => {
    component.productSearchQuery = 'demo';
    component.selectAllFiltered();
    expect(component.visibleProducts.every(p => p.selected)).toBe(true);
    component.selectAllFiltered();
    expect(component.visibleProducts.every(p => !p.selected)).toBe(true);
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
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    apiSpy.getProducts.and.returnValue(of([mockProduct]));
    const { fixture, component } = await configureWizard(apiSpy);
    fixture.detectChanges();

    component.setProductSource('export');

    expect(component.productSource).toBe('export');
    expect(component.products.length).toBe(0);
    expect(component.productsLoading).toBe(false);
  });

  it('importExportArchive_successReloadsProducts', async () => {
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    apiSpy.importExport.and.returnValue(of({
      importMode: 'export-v1',
      productCount: 1,
      products: [{ name: 'Seed Alpha', systemName: 'seed_alpha', serviceId: 1 }],
      manifest: { schemaVersion: '1.0', adminUrl: 'https://example.com', exportedAt: '2024-01-01' },
    }));
    apiSpy.getProducts.and.returnValue(of([exportProduct]));
    const { fixture, component } = await configureWizard(apiSpy);
    fixture.detectChanges();

    component.setProductSource('export');
    component.selectedExportFile = new File(['zip'], 'export-minimal.zip', { type: 'application/zip' });
    component.importExportArchive();
    fixture.detectChanges();

    expect(apiSpy.importExport).toHaveBeenCalled();
    expect(apiSpy.getProducts).toHaveBeenCalled();
    expect(component.products.length).toBe(1);
    expect(component.importResult?.productCount).toBe(1);
    expect(component.importing).toBe(false);
    expect(component.isExportProduct(component.products[0].product)).toBe(true);
  });

  it('importExportArchive_errorShowsMessage', async () => {
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    apiSpy.importExport.and.returnValue(throwError(() => ({ error: 'Only .zip export archives are supported' })));
    const { fixture, component } = await configureWizard(apiSpy);
    fixture.detectChanges();

    component.setProductSource('export');
    component.selectedExportFile = new File(['bad'], 'archive.tar.gz', { type: 'application/gzip' });
    component.importExportArchive();

    expect(component.importError).toContain('.zip');
    expect(apiSpy.importExport).not.toHaveBeenCalled();
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
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    apiSpy.getProducts.and.returnValue(of([mockProduct]));
    apiSpy.analyzeMigration.and.returnValue(of(mockPlan));
    const { fixture, component } = await configureWizard(apiSpy);
    fixture.detectChanges();

    component.products[0].selected = true;
    component.step = 2;
    component.gatewayStrategy = 'dual';
    component.analyze();

    expect(apiSpy.analyzeMigration).toHaveBeenCalledWith('dual', ['demo-api'], 'local');
    expect(component.step).toBe(3);
    expect(component.plan).toEqual(mockPlan);
    expect(component.analyzing).toBe(false);
  });

  it('analyze_skipsWhenAlreadyAnalyzing', async () => {
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    apiSpy.analyzeMigration.and.returnValue(of(mockPlan));
    const { component } = await configureWizard(apiSpy);

    component.analyzing = true;
    component.analyze();

    expect(apiSpy.analyzeMigration).not.toHaveBeenCalled();
  });
});

describe('MigrationWizardComponent review helpers', () => {
  let component: MigrationWizardComponent;

  beforeEach(async () => {
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);
    const configured = await configureWizard(apiSpy);
    component = configured.component;
  });

  it('hasConsumerApiKeySecrets_trueWhenSecretPresent', () => {
    component.plan = {
      id: 'p',
      gatewayStrategy: 'shared',
      sourceProducts: [],
      resources: [{ kind: 'Secret', name: 's', namespace: 'ns', yaml: 'kind: Secret' }],
      aiAnalysis: '',
      createdAt: '',
    };
    expect(component.hasConsumerApiKeySecrets).toBe(true);
    expect(component.consumerApiKeySecretCount).toBe(1);
  });

  it('hasOidcJwtAuth_trueWhenIssuerInYaml', () => {
    component.plan = {
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
    expect(component.hasOidcJwtAuth).toBe(true);
  });
});

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
    const apiSpy = createApiSpy();
    stubApiDefaults(apiSpy);

    await TestBed.configureTestingModule({
      imports: [MigrationWizardComponent],
      providers: [
        { provide: ApiService, useValue: apiSpy },
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
