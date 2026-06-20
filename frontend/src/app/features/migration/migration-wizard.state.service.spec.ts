import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MigrationWizardStateService } from './migration-wizard.state.service';
import { ClusterApiService } from '../../core/api/cluster-api.service';
import { MigrationApiService } from '../../core/api/migration-api.service';
import { ThreeScaleApiService } from '../../core/api/threescale-api.service';
import { MigrationPlan } from '../../core/api/models';

describe('MigrationWizardStateService', () => {
  let service: MigrationWizardStateService;
  let clusterApi: jasmine.SpyObj<ClusterApiService>;
  let migrationApi: jasmine.SpyObj<MigrationApiService>;
  let threescaleApi: jasmine.SpyObj<ThreeScaleApiService>;

  const mockPlan: MigrationPlan = {
    id: 'plan-1',
    gatewayStrategy: 'shared',
    sourceProducts: ['demo-api'],
    resources: [
      { kind: 'Gateway', name: 'gw', namespace: 'ns', yaml: 'kind: Gateway' },
      { kind: 'HTTPRoute', name: 'route', namespace: 'ns', yaml: 'kind: HTTPRoute' },
    ],
    aiAnalysis: 'ok',
    createdAt: '',
    targetClusterId: 'local',
    catalogInfoYaml: 'kind: Component',
  };

  beforeEach(() => {
    clusterApi = jasmine.createSpyObj<ClusterApiService>('ClusterApiService', [
      'getFeatures',
      'getTargetClusters',
      'getClusterReadiness',
    ]);
    migrationApi = jasmine.createSpyObj<MigrationApiService>('MigrationApiService', [
      'analyze',
      'importExport',
      'applyPlan',
      'revertPlan',
      'getPlans',
      'revertBulk',
      'checkDrift',
      'getTestCommands',
      'confirmRegistration',
    ]);
    threescaleApi = jasmine.createSpyObj<ThreeScaleApiService>('ThreeScaleApiService', ['getProducts']);

    clusterApi.getFeatures.and.returnValue(
      of({ developerHub: { enabled: true, url: 'https://hub.example.com' } }),
    );
    clusterApi.getTargetClusters.and.returnValue(of([]));
    clusterApi.getClusterReadiness.and.returnValue(
      of({
        clusterConnected: true,
        targetClusterId: 'local',
        connectionStatus: 'ok',
        prerequisites: [],
      }),
    );
    threescaleApi.getProducts.and.returnValue(of([]));

    TestBed.configureTestingModule({
      providers: [
        MigrationWizardStateService,
        { provide: ClusterApiService, useValue: clusterApi },
        { provide: MigrationApiService, useValue: migrationApi },
        { provide: ThreeScaleApiService, useValue: threescaleApi },
      ],
    });
    service = TestBed.inject(MigrationWizardStateService);
  });

  it('importExportArchive_validatesBeforeApiCall', () => {
    service.selectedExportFile = new File(['x'], 'bad.tar.gz');
    service.importExportArchive();
    expect(service.importError).toContain('.zip');
    expect(migrationApi.importExport).not.toHaveBeenCalled();
  });

  it('analyze_setsPlanAndAdvancesStep', () => {
    service.products = [{ product: { name: 'demo-api' } as never, selected: true }];
    migrationApi.analyze.and.returnValue(of(mockPlan));

    service.analyze();

    expect(migrationApi.analyze).toHaveBeenCalled();
    expect(service.step).toBe(3);
    expect(service.plan?.id).toBe('plan-1');
    expect(service.analyzing).toBe(false);
  });

  it('init_loadsFeaturesAndClusters', () => {
    service.init();
    expect(clusterApi.getFeatures).toHaveBeenCalled();
    expect(clusterApi.getTargetClusters).toHaveBeenCalled();
    expect(service.developerHubEnabled).toBe(true);
  });

  it('setProductSource_export_clearsLiveProducts', () => {
    service.products = [{ product: { name: 'demo-api' } as never, selected: false }];
    service.setProductSource('export');
    expect(service.products.length).toBe(0);
    expect(service.productsLoading).toBe(false);
  });

  it('onExportFileSelected_storesFile', () => {
    const file = new File(['zip'], 'export.zip');
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    service.onExportFileSelected({ target: input } as unknown as Event);
    expect(service.selectedExportFile).toBe(file);
  });

  it('refreshReadiness_updatesPlanPrerequisites', () => {
    service.plan = mockPlan;
    clusterApi.getClusterReadiness.and.returnValue(
      of({
        clusterConnected: true,
        targetClusterId: 'local',
        connectionStatus: 'ok',
        prerequisites: [
          {
            id: 'gw',
            category: 'connectivity',
            title: 'Gateway API',
            description: 'gw',
            requiredByPlan: true,
            optionalTier: false,
            status: 'satisfied',
            triggeredByCount: 1,
          },
        ],
      }),
    );

    service.refreshReadiness();
    expect(service.readinessLoading).toBe(false);
    expect(service.plan?.prerequisites?.length).toBe(1);
  });

  it('applyMigration_sendsExclusionsAndLoadsTestCommands', () => {
    service.plan = mockPlan;
    service.resourceEnabled = { 1: false };
    service.editedYamls = { '0': 'custom: yaml' };
    migrationApi.applyPlan.and.returnValue(
      of({ planId: 'plan-1', applied: 1, failed: 0, results: [] }),
    );
    migrationApi.getTestCommands.and.returnValue(of([{ label: 'curl', command: 'curl localhost' }]));

    service.applyMigration();

    expect(migrationApi.applyPlan).toHaveBeenCalledWith('plan-1', [1], { '0': 'custom: yaml' });
    expect(migrationApi.getTestCommands).toHaveBeenCalledWith('plan-1');
    expect(service.testCommands.length).toBe(1);
  });

  it('revertMigration_setsResult', () => {
    service.plan = mockPlan;
    migrationApi.revertPlan.and.returnValue(
      of({ planId: 'plan-1', applied: 1, failed: 0, results: [] }),
    );

    service.revertMigration();
    expect(service.revertResult?.applied).toBe(1);
  });

  it('toggleHistory_loadsPlansWhenOpened', () => {
    migrationApi.getPlans.and.returnValue(of([mockPlan]));
    service.toggleHistory();
    expect(service.historyOpen).toBe(true);
    expect(service.allPlans.length).toBe(1);
  });

  it('confirmBulkRevert_revertsSelectedPlans', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    service.allPlans = [mockPlan];
    service.planSelection = { 'plan-1': true };
    migrationApi.revertBulk.and.returnValue(
      of({ totalPlans: 1, totalReverted: 1, totalFailed: 0, planResults: [] }),
    );

    service.confirmBulkRevert();

    expect(migrationApi.revertBulk).toHaveBeenCalledWith(['plan-1'], false);
    expect(service.allPlans[0].status).toBe('REVERTED');
  });

  it('checkDrift_storesResults', () => {
    migrationApi.checkDrift.and.returnValue(
      of([{ kind: 'Gateway', name: 'gw', status: 'in-sync' }]),
    );
    service.checkDrift('plan-1');
    expect(service.driftResults['plan-1'].length).toBe(1);
    expect(service.driftLoading['plan-1']).toBe(false);
  });

  it('yamlHelpers_toggleEditAndReset', () => {
    service.plan = mockPlan;
    service.toggleYaml(0);
    expect(service.yamlOpen[0]).toBe(true);
    service.toggleEdit(0);
    expect(service.editMode[0]).toBe(true);
    service.onYamlEdit(0, { target: { value: 'edited' } } as unknown as Event);
    expect(service.editedYamls[0]).toBe('edited');
    service.resetYaml(0);
    expect(service.editedYamls[0]).toBeUndefined();
  });

  it('registerComponent_confirmsRegistration', () => {
    service.plan = mockPlan;
    migrationApi.confirmRegistration.and.returnValue(of({ status: 'ok' }));

    service.registerComponent();
    expect(migrationApi.confirmRegistration).toHaveBeenCalled();
    expect(service.registrationDone).toBe(true);
  });

  it('downloadAllYaml_generatesZipDownload', async () => {
    service.plan = mockPlan;
    const clickSpy = jasmine.createSpy('click');
    spyOn(URL, 'createObjectURL').and.returnValue('blob:mock');
    spyOn(URL, 'revokeObjectURL');
    spyOn(document, 'createElement').and.returnValue({
      click: clickSpy,
      download: '',
      href: '',
    } as unknown as HTMLAnchorElement);

    await service.downloadAllYaml();
    expect(clickSpy).toHaveBeenCalled();
  });

  it('reviewAndHistoryState_exposeSnapshots', () => {
    service.plan = mockPlan;
    service.historyOpen = true;
    expect(service.reviewState.plan.id).toBe('plan-1');
    expect(service.historyState.historyOpen).toBe(true);
  });

  it('analyze_errorResetsAnalyzingFlag', () => {
    service.products = [{ product: { name: 'demo-api' } as never, selected: true }];
    migrationApi.analyze.and.returnValue(throwError(() => new Error('fail')));
    service.analyze();
    expect(service.analyzing).toBe(false);
  });

  it('revertMigration_errorBuildsFallbackResult', () => {
    service.plan = mockPlan;
    migrationApi.revertPlan.and.returnValue(throwError(() => new Error('fail')));
    service.revertMigration();
    expect(service.revertResult?.failed).toBe(1);
  });

  it('reloadProductsAfterImport_errorSetsMessage', () => {
    migrationApi.importExport.and.returnValue(
      of({
        importMode: 'export-v1',
        productCount: 1,
        products: [],
        manifest: { schemaVersion: '1.0', adminUrl: '', exportedAt: '' },
      }),
    );
    threescaleApi.getProducts.and.returnValue(throwError(() => new Error('reload failed')));
    service.selectedExportFile = new File(['zip'], 'export.zip');
    service.importExportArchive();
    expect(service.importError).toContain('failed to reload');
  });

  it('reviewAndHistoryActions_coverDelegatedMethods', async () => {
    service.plan = mockPlan;
    service.allPlans = [mockPlan];
    service.planSelection = { 'plan-1': true };
    migrationApi.getPlans.and.returnValue(of([mockPlan]));
    migrationApi.revertBulk.and.returnValue(
      of({ totalPlans: 1, totalReverted: 1, totalFailed: 0, planResults: [] }),
    );
    migrationApi.checkDrift.and.returnValue(of([]));
    migrationApi.applyPlan.and.returnValue(
      of({ planId: 'plan-1', applied: 0, failed: 1, results: [] }),
    );
    migrationApi.revertPlan.and.returnValue(
      of({ planId: 'plan-1', applied: 0, failed: 1, results: [] }),
    );
    migrationApi.confirmRegistration.and.returnValue(of({ status: 'ok' }));
    migrationApi.getTestCommands.and.returnValue(of([]));
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(navigator.clipboard, 'writeText').and.returnValue(Promise.resolve());
    spyOn(URL, 'createObjectURL').and.returnValue('blob:mock');
    spyOn(URL, 'revokeObjectURL');
    spyOn(document, 'createElement').and.returnValue({
      click: () => undefined,
      download: '',
      href: '',
    } as unknown as HTMLAnchorElement);

    service.reviewActions.togglePrerequisites();
    service.reviewActions.toggleAiAnalysis();
    service.reviewActions.toggleYaml(0);
    service.reviewActions.toggleResourceEnabled(0);
    service.reviewActions.toggleEdit(0);
    service.reviewActions.onYamlEdit(0, { target: { value: 'edited' } } as unknown as Event);
    service.reviewActions.resetYaml(0);
    service.reviewActions.copyYaml(0);
    service.reviewActions.copyCatalogInfo();
    service.reviewActions.onComponentYamlEdit({ target: { value: 'component' } } as unknown as Event);
    service.reviewActions.toggleComponentEditMode();
    service.reviewActions.copyCommand('curl localhost');
    service.reviewActions.goBack();
    service.reviewActions.refreshReadiness();
    service.reviewActions.applyMigration();
    service.reviewActions.revertMigration();
    service.reviewActions.registerComponent();
    await service.reviewActions.downloadAllYaml();

    service.historyActions.toggleHistory();
    service.historyActions.toggleSelectAll();
    service.historyActions.togglePlanSelection('plan-1');
    service.historyActions.setDeleteGateway(true);
    service.historyActions.checkDrift('plan-1');
    service.historyActions.confirmBulkRevert();

    expect(service.step).toBe(2);
    expect(service.historyOpen).toBe(true);
  });

  it('applyMigration_errorBuildsFallbackResult', () => {
    service.plan = mockPlan;
    migrationApi.applyPlan.and.returnValue(throwError(() => new Error('fail')));
    service.applyMigration();
    expect(service.applyResult?.failed).toBe(1);
  });

  it('setProductSource_noopWhenUnchanged', () => {
    service.setProductSource('live');
    expect(threescaleApi.getProducts).not.toHaveBeenCalled();
  });

  it('onProductSearchChange_resetsPage', () => {
    service.migrateProductPage = 3;
    service.onProductSearchChange();
    expect(service.migrateProductPage).toBe(1);
  });

  it('toggleSelectAll_selectsActivePlans', () => {
    service.allPlans = [mockPlan, { ...mockPlan, id: 'plan-2', status: 'REVERTED' }];
    service.toggleSelectAll();
    expect(service.planSelection['plan-1']).toBe(true);
    expect(service.planSelection['plan-2']).toBeUndefined();
  });

  it('refreshReadiness_skipsPlanUpdateWhenEmptyPrerequisites', () => {
    service.plan = mockPlan;
    clusterApi.getClusterReadiness.and.returnValue(
      of({
        clusterConnected: true,
        targetClusterId: 'local',
        connectionStatus: 'ok',
        prerequisites: [],
      }),
    );
    service.refreshReadiness();
    expect(service.plan?.resources.length).toBe(2);
  });

  it('checkDrift_errorClearsLoading', () => {
    migrationApi.checkDrift.and.returnValue(throwError(() => new Error('drift failed')));
    service.checkDrift('plan-1');
    expect(service.driftLoading['plan-1']).toBe(false);
  });

  it('confirmBulkRevert_errorResetsFlag', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    service.planSelection = { 'plan-1': true };
    migrationApi.revertBulk.and.returnValue(throwError(() => new Error('bulk failed')));
    service.confirmBulkRevert();
    expect(service.bulkReverting).toBe(false);
  });

  it('registerComponent_errorSetsMessage', () => {
    service.plan = mockPlan;
    migrationApi.confirmRegistration.and.returnValue(
      throwError(() => ({ error: { message: 'hub down' }, message: 'fallback' })),
    );
    service.registerComponent();
    expect(service.registrationError).toBe('hub down');
  });

  it('loadTestCommands_errorClearsCommands', () => {
    service.plan = mockPlan;
    migrationApi.getTestCommands.and.returnValue(throwError(() => new Error('fail')));
    service.loadTestCommands();
    expect(service.testCommands).toEqual([]);
  });

  it('toggleHistory_loadErrorClearsLoading', () => {
    migrationApi.getPlans.and.returnValue(throwError(() => new Error('plans failed')));
    service.toggleHistory();
    expect(service.historyLoading).toBe(false);
  });

  it('copyCatalogInfo_noopWithoutYaml', () => {
    service.plan = { ...mockPlan, catalogInfoYaml: undefined };
    spyOn(navigator.clipboard, 'writeText');
    service.copyCatalogInfo();
    expect(navigator.clipboard.writeText).not.toHaveBeenCalled();
  });

  it('applyMigration_noopWithoutPlan', () => {
    service.plan = null;
    service.applyMigration();
    expect(migrationApi.applyPlan).not.toHaveBeenCalled();
  });

  it('confirmBulkRevert_abortsWhenUserDeclines', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    service.planSelection = { 'plan-1': true };
    service.confirmBulkRevert();
    expect(migrationApi.revertBulk).not.toHaveBeenCalled();
  });
});
