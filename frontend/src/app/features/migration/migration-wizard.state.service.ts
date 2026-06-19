import { Injectable } from '@angular/core';
import JSZip from 'jszip';
import { ClusterApiService } from '../../core/api/cluster-api.service';
import { MigrationApiService } from '../../core/api/migration-api.service';
import { ThreeScaleApiService } from '../../core/api/threescale-api.service';
import {
  ApplyResult,
  BulkRevertResult,
  DriftEntry,
  ImportExportResponse,
  MigrationPlan,
  TargetClusterView,
  TestCommand,
  ThreeScaleProduct,
} from '../../core/api/models';
import {
  WizardHistoryActions,
  WizardHistoryState,
  WizardReviewActions,
  WizardReviewState,
} from './steps/wizard-review.types';
import {
  GATEWAY_STRATEGIES,
  ProductRow,
  allFilteredSelected as computeAllFilteredSelected,
  allPlansSelected,
  consumerApiKeySecretCount,
  extractErrorMessage,
  groupPrerequisites,
  hasOidcJwtAuth,
  isExportProduct,
  pageProducts,
  productsLoadingMessage,
  selectedPlanIds,
  toggleAllFiltered,
  totalPages,
  validateExportFile,
  filterProductsByQuery,
} from './migration-wizard.helpers';

@Injectable()
export class MigrationWizardStateService {
  step = 1;
  stepLabels = ['Products', 'Strategy', 'Review'];
  products: ProductRow[] = [];
  productsLoading = true;
  gatewayStrategy = 'shared';
  analyzing = false;
  analyzeStage = 'Preparing analysis…';
  plan: MigrationPlan | null = null;
  prerequisitesOpen = true;
  aiAnalysisOpen = true;
  readinessLoading = false;
  yamlOpen: Record<number, boolean> = {};
  editMode: Record<number, boolean> = {};
  editedYamls: Record<number, string> = {};
  resourceEnabled: Record<number, boolean> = {};
  applying = false;
  applyResult: ApplyResult | null = null;
  reverting = false;
  revertResult: ApplyResult | null = null;
  catalogCopied = false;
  developerHubEnabled = false;
  developerHubUrl = '';
  componentEditMode = false;
  editedComponentYaml = '';
  registering = false;
  registrationDone = false;
  registrationError = '';
  testCommands: TestCommand[] = [];
  historyOpen = false;
  historyLoading = false;
  allPlans: MigrationPlan[] = [];
  planSelection: Record<string, boolean> = {};
  deleteGateway = false;
  bulkReverting = false;
  bulkResult: BulkRevertResult | null = null;
  driftResults: Record<string, DriftEntry[]> = {};
  driftLoading: Record<string, boolean> = {};
  productSearchQuery = '';
  migrateProductPage = 1;
  migratePageSize = 24;
  targetClusters: TargetClusterView[] = [];
  selectedClusterId = 'local';
  productSource: 'live' | 'export' = 'live';
  importing = false;
  importError = '';
  importResult: ImportExportResponse | null = null;
  selectedExportFile: File | null = null;

  readonly strategies = [...GATEWAY_STRATEGIES];
  readonly isExportProductFn = (product: ThreeScaleProduct) =>
    isExportProduct(product);

  readonly reviewActions: WizardReviewActions = {
    togglePrerequisites: () => this.togglePrerequisites(),
    refreshReadiness: () => this.refreshReadiness(),
    toggleAiAnalysis: () => this.toggleAiAnalysis(),
    toggleYaml: (index) => this.toggleYaml(index),
    toggleResourceEnabled: (index) => this.toggleResourceEnabled(index),
    toggleEdit: (index) => this.toggleEdit(index),
    copyYaml: (index) => this.copyYaml(index),
    resetYaml: (index) => this.resetYaml(index),
    onYamlEdit: (index, event) => this.onYamlEdit(index, event),
    downloadAllYaml: () => this.downloadAllYaml(),
    applyMigration: () => this.applyMigration(),
    revertMigration: () => this.revertMigration(),
    copyCatalogInfo: () => this.copyCatalogInfo(),
    onComponentYamlEdit: (event) => this.onComponentYamlEdit(event),
    toggleComponentEditMode: () => {
      this.componentEditMode = !this.componentEditMode;
    },
    registerComponent: () => this.registerComponent(),
    copyCommand: (command) => this.copyCommand(command),
    goBack: () => {
      this.step = 2;
    },
  };

  readonly historyActions: WizardHistoryActions = {
    toggleHistory: () => this.toggleHistory(),
    toggleSelectAll: () => this.toggleSelectAll(),
    togglePlanSelection: (planId) => this.togglePlanSelection(planId),
    confirmBulkRevert: () => this.confirmBulkRevert(),
    checkDrift: (planId) => this.checkDrift(planId),
    setDeleteGateway: (value) => {
      this.deleteGateway = value;
    },
  };

  constructor(
    private clusterApi: ClusterApiService,
    private migrationApi: MigrationApiService,
    private threescaleApi: ThreeScaleApiService,
  ) {}

  get reviewState(): WizardReviewState {
    return {
      plan: this.plan!,
      prerequisitesOpen: this.prerequisitesOpen,
      aiAnalysisOpen: this.aiAnalysisOpen,
      readinessLoading: this.readinessLoading,
      prerequisiteSections: this.prerequisiteSections,
      consumerApiKeySecretCount: this.consumerApiKeySecretCount,
      hasOidcJwtAuth: this.hasOidcJwtAuth,
      hasConsumerApiKeySecrets: this.hasConsumerApiKeySecrets,
      yamlOpen: this.yamlOpen,
      editMode: this.editMode,
      editedYamls: this.editedYamls,
      resourceEnabled: this.resourceEnabled,
      applying: this.applying,
      applyResult: this.applyResult,
      reverting: this.reverting,
      revertResult: this.revertResult,
      developerHubEnabled: this.developerHubEnabled,
      developerHubUrl: this.developerHubUrl,
      componentEditMode: this.componentEditMode,
      editedComponentYaml: this.editedComponentYaml,
      catalogCopied: this.catalogCopied,
      registering: this.registering,
      registrationDone: this.registrationDone,
      registrationError: this.registrationError,
      testCommands: this.testCommands,
    };
  }

  get historyState(): WizardHistoryState {
    return {
      historyOpen: this.historyOpen,
      historyLoading: this.historyLoading,
      allPlans: this.allPlans,
      planSelection: this.planSelection,
      deleteGateway: this.deleteGateway,
      bulkReverting: this.bulkReverting,
      bulkResult: this.bulkResult,
      driftResults: this.driftResults,
      driftLoading: this.driftLoading,
      allSelected: this.allSelected,
      selectedPlanIds: this.selectedPlanIds,
    };
  }

  get hasConsumerApiKeySecrets(): boolean {
    return this.consumerApiKeySecretCount > 0;
  }

  get hasOidcJwtAuth(): boolean {
    return hasOidcJwtAuth(this.plan);
  }

  get consumerApiKeySecretCount(): number {
    return consumerApiKeySecretCount(this.plan);
  }

  get prerequisiteSections() {
    return groupPrerequisites(this.plan?.prerequisites);
  }

  get selectedCount(): number {
    return this.products.filter((p) => p.selected).length;
  }

  get visibleProducts(): ProductRow[] {
    return filterProductsByQuery(this.products, this.productSearchQuery);
  }

  get migrateTotalPages(): number {
    return totalPages(this.visibleProducts.length, this.migratePageSize);
  }

  get pagedMigrateProducts(): ProductRow[] {
    return pageProducts(
      this.visibleProducts,
      this.migrateProductPage,
      this.migratePageSize,
    );
  }

  get allFilteredSelected(): boolean {
    return computeAllFilteredSelected(this.visibleProducts);
  }

  get productsLoadingMessage(): string {
    return productsLoadingMessage(this.importing, this.productSource);
  }

  get selectedPlanIds(): string[] {
    return selectedPlanIds(this.planSelection);
  }

  get allSelected(): boolean {
    return allPlansSelected(this.allPlans, this.planSelection);
  }

  init(): void {
    this.loadLiveProducts();
    this.clusterApi.getFeatures().subscribe({
      next: (f) => {
        this.developerHubEnabled = f.developerHub?.enabled ?? false;
        this.developerHubUrl = f.developerHub?.url ?? '';
      },
    });
    this.clusterApi.getTargetClusters().subscribe({
      next: (clusters) => {
        this.targetClusters = clusters;
      },
      error: () => {
        this.targetClusters = [
          {
            id: 'local',
            label: 'Local (in-cluster)',
            apiServerUrl: '',
            authType: 'in-cluster',
            verifySsl: true,
            enabled: true,
            credentialConfigured: false,
          },
        ];
      },
    });
  }

  onProductSearchChange(): void {
    this.migrateProductPage = 1;
  }

  selectAllFiltered(): void {
    toggleAllFiltered(this.visibleProducts);
  }

  setProductSource(source: 'live' | 'export'): void {
    if (this.productSource === source) return;
    this.productSource = source;
    this.importError = '';
    this.importResult = null;
    this.selectedExportFile = null;
    if (source === 'live') {
      this.loadLiveProducts();
    } else {
      this.products = [];
      this.productsLoading = false;
    }
  }

  loadLiveProducts(): void {
    this.productsLoading = true;
    this.threescaleApi.getProducts().subscribe({
      next: (data) => {
        this.products = data.map((p) => ({ product: p, selected: false }));
        this.productsLoading = false;
      },
      error: () => {
        this.productsLoading = false;
      },
    });
  }

  onExportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedExportFile = input.files?.[0] ?? null;
    this.importError = '';
  }

  importExportArchive(): void {
    const validationError = validateExportFile(this.selectedExportFile);
    if (validationError) {
      this.importError = validationError;
      return;
    }
    this.importing = true;
    this.importError = '';
    this.migrationApi.importExport(this.selectedExportFile!).subscribe({
      next: (result) => {
        this.importResult = result;
        this.reloadProductsAfterImport();
      },
      error: (err) => {
        this.importing = false;
        this.importError = extractErrorMessage(err);
      },
    });
  }

  private reloadProductsAfterImport(): void {
    this.productsLoading = true;
    this.threescaleApi.getProducts().subscribe({
      next: (data) => {
        this.products = data.map((p) => ({ product: p, selected: false }));
        this.productsLoading = false;
        this.importing = false;
      },
      error: () => {
        this.productsLoading = false;
        this.importing = false;
        this.importError = 'Import succeeded but failed to reload products';
      },
    });
  }

  refreshReadiness(): void {
    if (!this.plan) return;
    this.readinessLoading = true;
    this.clusterApi
      .getClusterReadiness(this.plan.targetClusterId, this.plan.id)
      .subscribe({
        next: (readiness) => {
          if (readiness.prerequisites?.length) {
            this.plan = { ...this.plan!, prerequisites: readiness.prerequisites };
          }
          this.readinessLoading = false;
        },
        error: () => {
          this.readinessLoading = false;
        },
      });
  }

  analyze(): void {
    if (this.analyzing) return;
    this.analyzing = true;
    this.analyzeStage = 'Reading selected products and gateway strategy…';
    this.applyResult = null;
    this.revertResult = null;
    const selected = this.products
      .filter((p) => p.selected)
      .map((p) => p.product.name);
    this.analyzeStage = 'Generating Connectivity Link resources…';
    this.migrationApi
      .analyze(this.gatewayStrategy, selected, this.selectedClusterId)
      .subscribe({
        next: (plan) => {
          this.analyzeStage = 'Finalizing migration plan…';
          this.plan = plan;
          this.yamlOpen = {};
          this.editMode = {};
          this.editedYamls = {};
          this.resourceEnabled = {};
          this.prerequisitesOpen = true;
          this.aiAnalysisOpen = true;
          this.step = 3;
          this.analyzing = false;
        },
        error: () => {
          this.analyzing = false;
        },
      });
  }

  applyMigration(): void {
    if (!this.plan) return;
    this.applying = true;
    const excludedIndexes = Object.entries(this.resourceEnabled)
      .filter(([, v]) => v === false)
      .map(([k]) => Number(k));
    const yamlOverrides: Record<string, string> = {};
    for (const [k, v] of Object.entries(this.editedYamls)) {
      if (v) yamlOverrides[k] = v;
    }
    this.migrationApi
      .applyPlan(this.plan.id, excludedIndexes, yamlOverrides)
      .subscribe({
        next: (result) => {
          this.applyResult = result;
          this.applying = false;
          this.loadTestCommands();
        },
        error: () => {
          this.applyResult = {
            planId: this.plan!.id,
            applied: 0,
            failed: 1,
            results: [
              {
                kind: 'Error',
                name: 'Apply failed',
                namespace: '',
                success: false,
                message: 'Backend communication error',
              },
            ],
          };
          this.applying = false;
        },
      });
  }

  revertMigration(): void {
    if (!this.plan) return;
    this.reverting = true;
    this.migrationApi.revertPlan(this.plan.id).subscribe({
      next: (result) => {
        this.revertResult = result;
        this.reverting = false;
      },
      error: () => {
        this.revertResult = {
          planId: this.plan!.id,
          applied: 0,
          failed: 1,
          results: [
            {
              kind: 'Error',
              name: 'Revert failed',
              namespace: '',
              success: false,
              message: 'Backend communication error',
            },
          ],
        };
        this.reverting = false;
      },
    });
  }

  toggleResourceEnabled(idx: number): void {
    const current = this.resourceEnabled[idx] !== false;
    this.resourceEnabled = { ...this.resourceEnabled, [idx]: !current };
  }

  toggleEdit(idx: number): void {
    this.editMode = { ...this.editMode, [idx]: !this.editMode[idx] };
  }

  onYamlEdit(idx: number, event: Event): void {
    const val = (event.target as HTMLTextAreaElement).value;
    this.editedYamls = { ...this.editedYamls, [idx]: val };
  }

  resetYaml(idx: number): void {
    const copy = { ...this.editedYamls };
    delete copy[idx];
    this.editedYamls = copy;
    this.editMode = { ...this.editMode, [idx]: false };
  }

  copyYaml(idx: number): void {
    if (!this.plan) return;
    const yaml = this.editedYamls[idx] || this.plan.resources[idx].yaml;
    navigator.clipboard.writeText(yaml);
  }

  toggleYaml(idx: number): void {
    this.yamlOpen = { ...this.yamlOpen, [idx]: !this.yamlOpen[idx] };
  }

  copyCatalogInfo(): void {
    const yaml = this.editedComponentYaml || this.plan?.catalogInfoYaml;
    if (!yaml) return;
    navigator.clipboard.writeText(yaml).then(() => {
      this.catalogCopied = true;
      setTimeout(() => (this.catalogCopied = false), 2000);
    });
  }

  onComponentYamlEdit(event: Event): void {
    this.editedComponentYaml = (event.target as HTMLTextAreaElement).value;
  }

  registerComponent(): void {
    if (!this.plan) return;
    this.registering = true;
    this.registrationError = '';
    const yaml = this.editedComponentYaml || this.plan.catalogInfoYaml || '';
    this.migrationApi.confirmRegistration(this.plan.id, yaml).subscribe({
      next: () => {
        this.registrationDone = true;
        this.registering = false;
      },
      error: (err) => {
        this.registering = false;
        this.registrationError =
          err.error?.message ||
          err.message ||
          'Registration failed. Check Developer Hub connectivity.';
      },
    });
  }

  copyCommand(cmd: string): void {
    navigator.clipboard.writeText(cmd);
  }

  loadTestCommands(): void {
    if (!this.plan) return;
    this.migrationApi.getTestCommands(this.plan.id).subscribe({
      next: (cmds) => (this.testCommands = cmds),
      error: () => (this.testCommands = []),
    });
  }

  togglePrerequisites(): void {
    this.prerequisitesOpen = !this.prerequisitesOpen;
  }

  toggleAiAnalysis(): void {
    this.aiAnalysisOpen = !this.aiAnalysisOpen;
  }

  toggleHistory(): void {
    this.historyOpen = !this.historyOpen;
    if (this.historyOpen && this.allPlans.length === 0) {
      this.historyLoading = true;
      this.migrationApi.getPlans().subscribe({
        next: (plans) => {
          this.allPlans = plans;
          this.historyLoading = false;
        },
        error: () => (this.historyLoading = false),
      });
    }
  }

  toggleSelectAll(): void {
    const allSel = this.allSelected;
    this.allPlans
      .filter((p) => p.status !== 'REVERTED')
      .forEach((p) => {
        this.planSelection[p.id] = !allSel;
      });
  }

  togglePlanSelection(id: string): void {
    this.planSelection = { ...this.planSelection, [id]: !this.planSelection[id] };
  }

  confirmBulkRevert(): void {
    const count = this.selectedPlanIds.length;
    if (
      !confirm(
        `This will delete Connectivity Link resources for ${count} plan(s). 3scale will resume routing. Continue?`,
      )
    ) {
      return;
    }
    this.bulkReverting = true;
    this.bulkResult = null;
    this.migrationApi
      .revertBulk(this.selectedPlanIds, this.deleteGateway)
      .subscribe({
        next: (result) => {
          this.bulkResult = result;
          this.bulkReverting = false;
          this.allPlans = this.allPlans.map((p) =>
            this.selectedPlanIds.includes(p.id)
              ? { ...p, status: 'REVERTED' }
              : p,
          );
          this.planSelection = {};
        },
        error: () => {
          this.bulkReverting = false;
        },
      });
  }

  checkDrift(planId: string): void {
    this.driftLoading = { ...this.driftLoading, [planId]: true };
    this.migrationApi.checkDrift(planId).subscribe({
      next: (results) => {
        this.driftResults = { ...this.driftResults, [planId]: results };
        this.driftLoading = { ...this.driftLoading, [planId]: false };
      },
      error: () => {
        this.driftLoading = { ...this.driftLoading, [planId]: false };
      },
    });
  }

  async downloadAllYaml(): Promise<void> {
    if (!this.plan) return;
    const zip = new JSZip();
    const folder = zip.folder(`gateforge-plan-${this.plan.id}`);
    if (!folder) return;

    const kindOrder: Record<string, number> = {
      Gateway: 0,
      HTTPRoute: 1,
      APIProduct: 2,
      PlanPolicy: 3,
      Secret: 4,
      AuthPolicy: 5,
      RateLimitPolicy: 6,
      TelemetryPolicy: 7,
      Route: 8,
      APIKey: 9,
    };

    const sorted = [...this.plan.resources].sort(
      (a, b) => (kindOrder[a.kind] ?? 99) - (kindOrder[b.kind] ?? 99),
    );

    const resources: string[] = [];
    sorted.forEach((res, idx) => {
      const yaml =
        this.editedYamls[this.plan!.resources.indexOf(res)] || res.yaml;
      const prefix = String(idx).padStart(2, '0');
      const safeName = res.name.toLowerCase().replace(/[^a-z0-9-]/g, '-');
      const filename = `${prefix}-${res.kind.toLowerCase()}-${safeName}.yaml`;
      folder.file(filename, yaml);
      resources.push(filename);
    });

    if (this.plan.catalogInfoYaml) {
      const catalogYaml = this.editedComponentYaml || this.plan.catalogInfoYaml;
      folder.file('catalog-info.yaml', catalogYaml);
      resources.push('catalog-info.yaml');
    }

    const kustomization = `apiVersion: kustomize.config.k8s.io/v1beta1\nkind: Kustomization\nresources:\n${resources.map((r) => '  - ' + r).join('\n')}\n`;
    folder.file('kustomization.yaml', kustomization);

    const blob = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `gateforge-plan-${this.plan.id}.zip`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
