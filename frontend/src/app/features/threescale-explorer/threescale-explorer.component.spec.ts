import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ThreeScaleExplorerComponent } from './threescale-explorer.component';
import { ThreeScaleApiService } from '../../core/api/threescale-api.service';
import { ThreeScaleBackend, ThreeScaleProduct, ThreeScaleStatus } from '../../core/api/models';

const mockProduct: ThreeScaleProduct = {
  name: 'demo-api',
  namespace: 'default',
  systemName: 'demo-api',
  description: 'Demo API',
  deploymentOption: 'hosted',
  mappingRules: [{ httpMethod: 'GET', pattern: '/', metricRef: 'hits', delta: 1 }],
  backendUsages: [{ backendName: 'api', path: '/' }],
  authentication: { authType: 'api_key' },
  source: 'CRD',
};

const apiProduct: ThreeScaleProduct = {
  ...mockProduct,
  name: 'billing-api',
  systemName: 'billing_api',
  source: 'Admin API',
  mappingRules: [],
  authentication: {},
};

const mockBackend: ThreeScaleBackend = {
  name: 'api',
  namespace: 'default',
  systemName: 'api',
  source: 'Admin API',
  privateEndpoint: 'http://backend',
  description: 'Billing backend',
};

const crdBackend: ThreeScaleBackend = {
  ...mockBackend,
  name: 'crd-backend',
  source: 'CRD',
};

const mockStatus: ThreeScaleStatus = {
  configured: true,
  reachable: true,
  crdDiscoveryEnabled: true,
  productCount: 2,
};

describe('ThreeScaleExplorerComponent', () => {
  let fixture: ComponentFixture<ThreeScaleExplorerComponent>;
  let threescaleApi: jasmine.SpyObj<ThreeScaleApiService>;

  beforeEach(async () => {
    threescaleApi = jasmine.createSpyObj<ThreeScaleApiService>('ThreeScaleApiService', [
      'getStatus',
      'getProducts',
      'getBackends',
      'refreshDiscovery',
    ]);
    threescaleApi.getStatus.and.returnValue(of(mockStatus));
    threescaleApi.getProducts.and.returnValue(of([mockProduct, apiProduct]));
    threescaleApi.getBackends.and.returnValue(of([mockBackend, crdBackend]));
    threescaleApi.refreshDiscovery.and.returnValue(
      of({ productCount: 2, backendCount: 2, message: 'ok' }),
    );

    await TestBed.configureTestingModule({
      imports: [ThreeScaleExplorerComponent],
      providers: [
        { provide: ThreeScaleApiService, useValue: threescaleApi },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ThreeScaleExplorerComponent);
  });

  it('loadsProductsAndBackendsOnInit', () => {
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.loading).toBe(false);
    expect(component.products.length).toBe(2);
    expect(component.backends.length).toBe(2);
    expect(component.crdCount).toBe(1);
    expect(component.apiCount).toBe(1);
    expect(component.totalMappingRules).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('demo-api');
  });

  it('filtersByTabSearchAndPagination', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.pageSize = 1;

    component.setTab('crd');
    expect(component.filteredProducts.length).toBe(1);
    expect(component.filteredBackends.length).toBe(1);

    component.setTab('admin-api');
    expect(component.filteredProducts.length).toBe(1);
    expect(component.filteredBackends.length).toBe(1);

    component.setTab('all');
    component.searchQuery = 'billing';
    component.onSearchChange();
    expect(component.filteredProducts.length).toBe(1);
    expect(component.filteredBackends.some(b => (b.description || '').includes('Billing'))).toBe(true);

    component.searchQuery = '';
    component.pageSize = 1;
    expect(component.productTotalPages).toBe(2);
    component.productPage = 2;
    expect(component.pagedProducts.length).toBe(1);
    component.backendPage = 2;
    expect(component.pagedBackends.length).toBe(1);
  });

  it('togglesProductExpansionAndAuthPanel', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.hasAuth(mockProduct)).toBe(true);
    expect(component.hasAuth(apiProduct)).toBe(false);

    component.toggleExpand(mockProduct);
    expect(component.expandedKey).toBe(component.productKey(mockProduct));

    component.toggleExpand(mockProduct, new Event('click'));
    expect(component.expandedKey).toBeNull();
    expect(component.trackByName(0, mockProduct)).toContain('demo-api');
  });

  it('refreshDiscoveryReloadsDataAndHandlesErrors', () => {
    fixture.detectChanges();
    fixture.componentInstance.refreshDiscovery();
    expect(fixture.componentInstance.refreshMessage).toContain('Updated');

    threescaleApi.refreshDiscovery.and.returnValue(throwError(() => new Error('refresh failed')));
    fixture.componentInstance.refreshDiscovery();
    expect(fixture.componentInstance.refreshMessage).toContain('failed');
  });

  it('handlesInitialLoadErrors', () => {
    threescaleApi.getStatus.and.returnValue(throwError(() => new Error('status failed')));
    threescaleApi.getProducts.and.returnValue(throwError(() => new Error('products failed')));
    threescaleApi.getBackends.and.returnValue(throwError(() => new Error('backends failed')));
    fixture.detectChanges();
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('rendersEmptyStatesPerTab', () => {
    threescaleApi.getProducts.and.returnValue(of([]));
    threescaleApi.getBackends.and.returnValue(of([]));
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.setTab('crd');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No CRD-based products');

    component.setTab('admin-api');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('THREESCALE_ADMIN_URL');
  });

  it('exposesTabCounts', () => {
    fixture.detectChanges();
    const tabs = fixture.componentInstance.tabs;
    expect(tabs.length).toBe(3);
    expect(tabs[0].count).toBe(4);
  });

  it('rendersDisconnectedAdminApiStatus', () => {
    threescaleApi.getStatus.and.returnValue(
      of({ configured: false, reachable: false, crdDiscoveryEnabled: false, productCount: 0 }),
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Not configured');
  });

  it('refreshDiscovery_ignoresDuplicateRequests', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.refreshing = true;
    component.refreshDiscovery();
    expect(threescaleApi.refreshDiscovery).not.toHaveBeenCalled();
  });

  it('refreshDiscovery_handlesReloadFailure', () => {
    fixture.detectChanges();
    threescaleApi.refreshDiscovery.and.returnValue(of({ productCount: 1, backendCount: 1, message: 'ok' }));
    threescaleApi.getProducts.and.returnValue(throwError(() => new Error('reload failed')));
    fixture.componentInstance.refreshDiscovery();
    expect(fixture.componentInstance.refreshMessage).toContain('failed');
  });

  it('showsAutoPathDiscoveryWhenNoMappingRules', () => {
    threescaleApi.getProducts.and.returnValue(of([{
      ...apiProduct,
      mappingRules: [],
    }]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Path Discovery');
  });

  it('rendersUnreachableAdminApiStatus', () => {
    threescaleApi.getStatus.and.returnValue(
      of({ configured: true, reachable: false, crdDiscoveryEnabled: true, productCount: 0 }),
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unreachable');
  });

  it('rendersMergedSourceBadgeWhenExpanded', () => {
    threescaleApi.getProducts.and.returnValue(of([{
      ...mockProduct,
      source: 'CRD + Admin API',
      mappingRules: [],
      authentication: {},
    }]));
    fixture.detectChanges();
    fixture.componentInstance.toggleExpand({
      ...mockProduct,
      source: 'CRD + Admin API',
      mappingRules: [],
      authentication: {},
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('CRD + Admin API');
    expect(fixture.nativeElement.textContent).toContain('No explicit mapping rules');
  });
});
