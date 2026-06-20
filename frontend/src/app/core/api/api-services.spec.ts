import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ClusterApiService } from './cluster-api.service';
import { MigrationApiService } from './migration-api.service';
import { ThreeScaleApiService } from './threescale-api.service';
import type { MigrationPlan } from './models';

describe('ClusterApiService', () => {
  let service: ClusterApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ClusterApiService],
    });
    service = TestBed.inject(ClusterApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getTargetClusters_requestsCorrectUrl', () => {
    service.getTargetClusters().subscribe(clusters => {
      expect(clusters[0].id).toBe('local');
    });

    const req = httpMock.expectOne('/api/cluster/targets');
    expect(req.request.method).toBe('GET');
    req.flush([{
      id: 'local',
      label: 'Local',
      apiServerUrl: '',
      authType: 'in-cluster',
      verifySsl: true,
      enabled: true,
      credentialConfigured: false,
    }]);
  });

  it('getFeatures_requestsCorrectUrl', () => {
    service.getFeatures().subscribe(features => {
      expect(features.developerHub?.enabled).toBe(false);
    });

    const req = httpMock.expectOne('/api/cluster/features');
    expect(req.request.method).toBe('GET');
    req.flush({ developerHub: { enabled: false, url: '' } });
  });

  it('getClusterReadiness_buildsQuery', () => {
    service.getClusterReadiness('lab', 'plan-abc').subscribe();

    const req = httpMock.expectOne('/api/cluster/readiness?targetClusterId=lab&planId=plan-abc');
    expect(req.request.method).toBe('GET');
    req.flush({
      clusterConnected: true,
      targetClusterId: 'lab',
      connectionStatus: 'ok',
      prerequisites: [],
    });
  });

  it('getClusterReadiness_withoutParams', () => {
    service.getClusterReadiness().subscribe();
    const req = httpMock.expectOne('/api/cluster/readiness');
    expect(req.request.method).toBe('GET');
    req.flush({
      clusterConnected: true,
      targetClusterId: 'local',
      connectionStatus: 'ok',
      prerequisites: [],
    });
  });

  it('validateTargetCluster_requestsCorrectUrl', () => {
    service.validateTargetCluster('lab').subscribe(result => {
      expect(result['connected']).toBe(true);
    });

    const req = httpMock.expectOne('/api/cluster/targets/lab/validate');
    expect(req.request.method).toBe('GET');
    req.flush({ connected: true });
  });
});

describe('ThreeScaleApiService', () => {
  let service: ThreeScaleApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ThreeScaleApiService],
    });
    service = TestBed.inject(ThreeScaleApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getProducts_requestsCorrectUrl', () => {
    service.getProducts().subscribe(products => {
      expect(products.length).toBe(1);
      expect(products[0].name).toBe('demo-api');
    });

    const req = httpMock.expectOne('/api/threescale/products');
    expect(req.request.method).toBe('GET');
    req.flush([{
      name: 'demo-api',
      namespace: 'default',
      systemName: 'demo-api',
      description: 'Demo',
      deploymentOption: 'hosted',
      mappingRules: [],
      backendUsages: [],
      authentication: {},
      source: 'local',
    }]);
  });

  it('getSourceStatus_requestsCorrectUrl', () => {
    service.getSourceStatus('lab').subscribe(status => {
      expect(status.reachable).toBe(true);
    });

    const req = httpMock.expectOne('/api/threescale/sources/lab/status');
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'lab',
      label: 'Lab',
      adminUrl: 'https://3scale.example.com',
      configured: true,
      enabled: true,
      reachable: true,
    });
  });

  it('getBackends_requestsCorrectUrl', () => {
    service.getBackends().subscribe(backends => {
      expect(backends.length).toBe(1);
    });

    const req = httpMock.expectOne('/api/threescale/backends');
    expect(req.request.method).toBe('GET');
    req.flush([{ name: 'api', source: 'CRD' }]);
  });

  it('getStatus_requestsCorrectUrl', () => {
    service.getStatus().subscribe(status => {
      expect(status.reachable).toBe(true);
    });

    const req = httpMock.expectOne('/api/threescale/status');
    expect(req.request.method).toBe('GET');
    req.flush({ configured: true, reachable: true, crdDiscoveryEnabled: true });
  });

  it('refreshDiscovery_postsRefresh', () => {
    service.refreshDiscovery().subscribe(result => {
      expect(result.productCount).toBe(2);
    });

    const req = httpMock.expectOne('/api/threescale/refresh');
    expect(req.request.method).toBe('POST');
    req.flush({ productCount: 2, backendCount: 1, message: 'ok' });
  });

  it('getSources_requestsCorrectUrl', () => {
    service.getSources().subscribe(sources => {
      expect(sources.length).toBe(1);
    });

    const req = httpMock.expectOne('/api/threescale/sources');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'default', label: 'Default', adminUrl: 'https://3scale.example.com', enabled: true }]);
  });

  it('addSource_postsBody', () => {
    service.addSource({
      id: 'prod',
      label: 'Production',
      adminUrl: 'https://3scale.example.com',
      enabled: true,
      accessToken: 'token',
    }).subscribe(source => {
      expect(source.id).toBe('prod');
    });

    const req = httpMock.expectOne('/api/threescale/sources');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'prod', label: 'Production', adminUrl: 'https://3scale.example.com', enabled: true });
  });

  it('removeSource_deletesById', () => {
    service.removeSource('prod').subscribe();
    const req = httpMock.expectOne('/api/threescale/sources/prod');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});

describe('MigrationApiService', () => {
  let service: MigrationApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MigrationApiService],
    });
    service = TestBed.inject(MigrationApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze_postsBody', () => {
    const plan: MigrationPlan = {
      id: 'plan-1',
      gatewayStrategy: 'shared',
      sourceProducts: ['demo-api'],
      resources: [],
      aiAnalysis: 'ok',
      createdAt: new Date().toISOString(),
    };

    service.analyze('shared', ['demo-api'], 'local').subscribe(result => {
      expect(result.id).toBe('plan-1');
    });

    const req = httpMock.expectOne('/api/migration/analyze');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      gatewayStrategy: 'shared',
      products: ['demo-api'],
      targetClusterId: 'local',
    });
    req.flush(plan);
  });

  it('analyze_defaultsTargetClusterToLocal', () => {
    const plan: MigrationPlan = {
      id: 'plan-1',
      gatewayStrategy: 'shared',
      sourceProducts: ['demo-api'],
      resources: [],
      aiAnalysis: 'ok',
      createdAt: new Date().toISOString(),
    };

    service.analyze('shared', ['demo-api']).subscribe();

    const req = httpMock.expectOne('/api/migration/analyze');
    expect(req.request.body.targetClusterId).toBe('local');
    req.flush(plan);
  });

  it('applyPlan_postsExclusionsAndOverrides', () => {
    service.applyPlan('plan-1', [0, 2], { '1': 'custom: yaml' }).subscribe(result => {
      expect(result.applied).toBe(1);
    });

    const req = httpMock.expectOne('/api/migration/plans/plan-1/apply');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      excludedIndexes: [0, 2],
      yamlOverrides: { '1': 'custom: yaml' },
    });
    req.flush({ planId: 'plan-1', applied: 1, failed: 0, results: [] });
  });

  it('applyPlan_postsEmptyBodyWhenNoOverrides', () => {
    service.applyPlan('plan-1').subscribe();
    const req = httpMock.expectOne('/api/migration/plans/plan-1/apply');
    expect(req.request.body).toEqual({});
    req.flush({ planId: 'plan-1', applied: 1, failed: 0, results: [] });
  });

  it('getPlans_requestsCorrectUrl', () => {
    service.getPlans().subscribe(plans => {
      expect(plans.length).toBe(0);
    });

    const req = httpMock.expectOne('/api/migration/plans');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('importExport_postsMultipartFile', () => {
    const file = new File(['zip'], 'export-minimal.zip', { type: 'application/zip' });

    service.importExport(file).subscribe(result => {
      expect(result.importMode).toBe('export-v1');
      expect(result.productCount).toBe(2);
    });

    const req = httpMock.expectOne('/api/migration/import-export');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('file')).toBeTruthy();
    req.flush({
      importMode: 'export-v1',
      productCount: 2,
      products: [{ name: 'Seed Alpha', systemName: 'seed_alpha', serviceId: 1 }],
      manifest: { schemaVersion: '1.0', adminUrl: 'https://example.com', exportedAt: '2024-01-01' },
    });
  });

  it('checkDrift_requestsCorrectUrl', () => {
    service.checkDrift('plan-1').subscribe(entries => {
      expect(entries.length).toBe(1);
    });

    const req = httpMock.expectOne('/api/migration/plans/plan-1/drift');
    expect(req.request.method).toBe('GET');
    req.flush([{ kind: 'Gateway', name: 'gw', status: 'in-sync' }]);
  });

  it('revertPlan_postsRevert', () => {
    service.revertPlan('plan-1').subscribe(result => {
      expect(result.applied).toBe(1);
    });

    const req = httpMock.expectOne('/api/migration/plans/plan-1/revert');
    expect(req.request.method).toBe('POST');
    req.flush({ planId: 'plan-1', applied: 1, failed: 0, results: [] });
  });

  it('revertBulk_postsPlanIds', () => {
    service.revertBulk(['plan-1'], true).subscribe(result => {
      expect(result.totalReverted).toBe(1);
    });

    const req = httpMock.expectOne('/api/migration/revert-bulk');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ planIds: ['plan-1'], deleteGateway: true });
    req.flush({ totalPlans: 1, totalReverted: 1, totalFailed: 0, planResults: [] });
  });

  it('getTestCommands_requestsCorrectUrl', () => {
    service.getTestCommands('plan-1').subscribe(commands => {
      expect(commands.length).toBe(1);
    });

    const req = httpMock.expectOne('/api/migration/plans/plan-1/test-commands');
    expect(req.request.method).toBe('GET');
    req.flush([{ label: 'curl', command: 'curl localhost' }]);
  });

  it('confirmRegistration_postsYaml', () => {
    service.confirmRegistration('plan-1', 'kind: Component').subscribe(result => {
      expect(result['status']).toBe('ok');
    });

    const req = httpMock.expectOne('/api/migration/plans/plan-1/confirm-registration');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ componentYaml: 'kind: Component' });
    req.flush({ status: 'ok' });
  });

  it('getCatalogInfo_requestsText', () => {
    service.getCatalogInfo('plan-1', 'demo-api').subscribe(yaml => {
      expect(yaml).toContain('Component');
    });

    const req = httpMock.expectOne('/api/migration/plans/plan-1/catalog-info/demo-api');
    expect(req.request.method).toBe('GET');
    req.flush('kind: Component', { status: 200, statusText: 'OK' });
  });
});
