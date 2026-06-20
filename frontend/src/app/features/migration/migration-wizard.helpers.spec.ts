import {
  allFilteredSelected,
  allPlansSelected,
  consumerApiKeySecretCount,
  extractErrorMessage,
  filterProductsByQuery,
  groupPrerequisites,
  hasOidcJwtAuth,
  isExportProduct,
  pageProducts,
  productsLoadingMessage,
  selectedPlanIds,
  totalPages,
  toggleAllFiltered,
  validateExportFile,
} from './migration-wizard.helpers';
import { MigrationPlan, ThreeScaleProduct } from '../../core/api/models';

const mockProduct = (name: string): ThreeScaleProduct => ({
  name,
  namespace: 'default',
  systemName: name,
  description: name,
  deploymentOption: 'hosted',
  mappingRules: [],
  backendUsages: [],
  authentication: {},
  source: 'local',
});

describe('migration-wizard.helpers', () => {
  it('filterProductsByQuery_matchesNameOrNamespace', () => {
    const rows = [
      { product: mockProduct('demo-api'), selected: false },
      { product: mockProduct('other-api'), selected: false },
    ];
    expect(filterProductsByQuery(rows, 'other').length).toBe(1);
    expect(filterProductsByQuery(rows, '').length).toBe(2);
  });

  it('pageProducts_and_totalPages', () => {
    const rows = Array.from({ length: 5 }, (_, i) => ({
      product: mockProduct(`p${i}`),
      selected: false,
    }));
    expect(totalPages(rows.length, 2)).toBe(3);
    expect(pageProducts(rows, 2, 2).map((r) => r.product.name)).toEqual([
      'p2',
      'p3',
    ]);
  });

  it('toggleAllFiltered_selectsAndClears', () => {
    const rows = [
      { product: mockProduct('a'), selected: false },
      { product: mockProduct('b'), selected: false },
    ];
    toggleAllFiltered(rows);
    expect(allFilteredSelected(rows)).toBe(true);
    toggleAllFiltered(rows);
    expect(allFilteredSelected(rows)).toBe(false);
  });

  it('groupPrerequisites_ordersByCategory', () => {
    const sections = groupPrerequisites([
      {
        id: 'rhcl-core',
        category: 'core-policy',
        title: 'RHCL',
        description: 'core',
        requiredByPlan: true,
        optionalTier: false,
        status: 'satisfied',
        triggeredByCount: 1,
      },
      {
        id: 'gateway-api',
        category: 'connectivity',
        title: 'Gateway API',
        description: 'gw',
        requiredByPlan: true,
        optionalTier: false,
        status: 'unknown',
        triggeredByCount: 1,
      },
    ]);
    expect(sections[0].category).toBe('connectivity');
    expect(sections[1].category).toBe('core-policy');
  });

  it('planReviewHelpers_detectSecretsAndOidc', () => {
    const plan: MigrationPlan = {
      id: 'p',
      gatewayStrategy: 'shared',
      sourceProducts: [],
      resources: [
        { kind: 'Secret', name: 's', namespace: 'ns', yaml: '' },
        {
          kind: 'AuthPolicy',
          name: 'a',
          namespace: 'ns',
          yaml: 'issuerUrl: https://sso.example.com',
        },
      ],
      aiAnalysis: '',
      createdAt: '',
    };
    expect(consumerApiKeySecretCount(plan)).toBe(1);
    expect(hasOidcJwtAuth(plan)).toBe(true);
  });

  it('isExportProduct_checksSource', () => {
    expect(isExportProduct({ ...mockProduct('x'), source: 'export-v1 (url)' })).toBe(
      true,
    );
    expect(isExportProduct(mockProduct('x'))).toBe(false);
  });

  it('filterProductsByQuery_matchesBackendNamespaceAndSystemName', () => {
    const rows = [
      {
        product: {
          ...mockProduct('demo-api'),
          backendNamespace: 'billing',
          systemName: 'billing_api',
        },
        selected: false,
      },
    ];
    expect(filterProductsByQuery(rows, 'billing').length).toBe(1);
    expect(filterProductsByQuery(rows, 'billing_api').length).toBe(1);
  });

  it('validateExportFile_rejectsMissingOrNonZip', () => {
    expect(validateExportFile(null)).toContain('.zip');
    expect(
      validateExportFile(new File(['x'], 'archive.tar.gz', { type: 'application/gzip' })),
    ).toContain('.zip');
    expect(validateExportFile(new File(['x'], 'export.zip'))).toBeNull();
  });

  it('productsLoadingMessage_reflectsImportState', () => {
    expect(productsLoadingMessage(true, 'live')).toContain('Importing');
    expect(productsLoadingMessage(false, 'export')).toContain('imported');
    expect(productsLoadingMessage(false, 'live')).toContain('cluster');
  });

  it('extractErrorMessage_readsStringBodyObjectOrFallback', () => {
    expect(extractErrorMessage({ error: 'bad zip' })).toBe('bad zip');
    expect(extractErrorMessage({ error: { message: 'server error' } })).toBe('server error');
    expect(extractErrorMessage({ message: 'network' })).toBe('network');
    expect(extractErrorMessage({})).toBe('Import failed');
  });

  it('consumerApiKeySecretCount_handlesNullPlan', () => {
    expect(consumerApiKeySecretCount(null)).toBe(0);
    expect(hasOidcJwtAuth(null)).toBe(false);
  });

  it('groupPrerequisites_returnsEmptyForMissingList', () => {
    expect(groupPrerequisites(undefined)).toEqual([]);
  });

  it('allPlansSelected_falseWhenNoActivePlans', () => {
    expect(allPlansSelected([], {})).toBe(false);
  });

  it('allFilteredSelected_falseForEmptyList', () => {
    expect(allFilteredSelected([])).toBe(false);
  });

  it('hasOidcJwtAuth_falseWithoutIssuerUrl', () => {
    const plan: MigrationPlan = {
      id: 'p',
      gatewayStrategy: 'shared',
      sourceProducts: [],
      resources: [{ kind: 'AuthPolicy', name: 'a', namespace: 'ns', yaml: 'mode: jwt' }],
      aiAnalysis: '',
      createdAt: '',
    };
    expect(hasOidcJwtAuth(plan)).toBe(false);
  });

  it('groupPrerequisites_usesCategoryFallbackLabel', () => {
    const sections = groupPrerequisites([
      {
        id: 'custom',
        category: 'custom-category',
        title: 'Custom',
        description: 'custom',
        requiredByPlan: true,
        optionalTier: false,
        status: 'unknown',
        triggeredByCount: 0,
      },
    ]);
    expect(sections).toEqual([]);
  });

  it('planSelectionHelpers_trackSelectedIds', () => {
    const plans: MigrationPlan[] = [
      {
        id: 'a',
        gatewayStrategy: 'shared',
        sourceProducts: [],
        resources: [],
        aiAnalysis: '',
        createdAt: '',
      },
      {
        id: 'b',
        gatewayStrategy: 'shared',
        sourceProducts: [],
        resources: [],
        aiAnalysis: '',
        createdAt: '',
        status: 'REVERTED',
      },
    ];
    const selection = { a: true, b: true };
    expect(selectedPlanIds(selection)).toEqual(['a', 'b']);
    expect(allPlansSelected(plans, selection)).toBe(true);
    expect(allPlansSelected(plans, { a: false })).toBe(false);
  });
});
