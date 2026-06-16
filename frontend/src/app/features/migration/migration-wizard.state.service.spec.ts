import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { MigrationWizardStateService } from './migration-wizard.state.service';
import { ClusterApiService } from '../../core/api/cluster-api.service';
import { MigrationApiService } from '../../core/api/migration-api.service';
import { ThreeScaleApiService } from '../../core/api/threescale-api.service';

describe('MigrationWizardStateService', () => {
  let service: MigrationWizardStateService;
  let migrationApi: jasmine.SpyObj<MigrationApiService>;

  beforeEach(() => {
    migrationApi = jasmine.createSpyObj<MigrationApiService>('MigrationApiService', [
      'analyze',
      'importExport',
    ]);
    TestBed.configureTestingModule({
      providers: [
        MigrationWizardStateService,
        {
          provide: ThreeScaleApiService,
          useValue: jasmine.createSpyObj('ThreeScaleApiService', ['getProducts']),
        },
        {
          provide: ClusterApiService,
          useValue: jasmine.createSpyObj('ClusterApiService', [
            'getFeatures',
            'getTargetClusters',
          ]),
        },
        { provide: MigrationApiService, useValue: migrationApi },
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
    migrationApi.analyze.and.returnValue(
      of({
        id: 'plan-1',
        gatewayStrategy: 'shared',
        sourceProducts: ['demo-api'],
        resources: [],
        aiAnalysis: '',
        createdAt: '',
      }),
    );

    service.analyze();

    expect(migrationApi.analyze).toHaveBeenCalled();
    expect(service.step).toBe(3);
    expect(service.plan?.id).toBe('plan-1');
    expect(service.analyzing).toBe(false);
  });
});
