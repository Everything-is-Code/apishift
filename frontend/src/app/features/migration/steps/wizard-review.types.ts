import {
  ApplyResult,
  BulkRevertResult,
  DriftEntry,
  MigrationPlan,
  MigrationPrerequisite,
  TestCommand,
} from '../../../core/api/models';

export interface PrerequisiteSection {
  category: string;
  label: string;
  items: MigrationPrerequisite[];
}

export interface WizardReviewState {
  plan: MigrationPlan;
  prerequisitesOpen: boolean;
  aiAnalysisOpen: boolean;
  readinessLoading: boolean;
  prerequisiteSections: PrerequisiteSection[];
  consumerApiKeySecretCount: number;
  hasOidcJwtAuth: boolean;
  hasConsumerApiKeySecrets: boolean;
  yamlOpen: Record<number, boolean>;
  editMode: Record<number, boolean>;
  editedYamls: Record<number, string>;
  resourceEnabled: Record<number, boolean>;
  applying: boolean;
  applyResult: ApplyResult | null;
  reverting: boolean;
  revertResult: ApplyResult | null;
  developerHubEnabled: boolean;
  developerHubUrl: string;
  componentEditMode: boolean;
  editedComponentYaml: string;
  catalogCopied: boolean;
  registering: boolean;
  registrationDone: boolean;
  registrationError: string;
  testCommands: TestCommand[];
}

export interface WizardReviewActions {
  togglePrerequisites(): void;
  refreshReadiness(): void;
  toggleAiAnalysis(): void;
  toggleYaml(index: number): void;
  toggleResourceEnabled(index: number): void;
  toggleEdit(index: number): void;
  copyYaml(index: number): void;
  resetYaml(index: number): void;
  onYamlEdit(index: number, event: Event): void;
  downloadAllYaml(): void;
  applyMigration(): void;
  revertMigration(): void;
  copyCatalogInfo(): void;
  onComponentYamlEdit(event: Event): void;
  toggleComponentEditMode(): void;
  registerComponent(): void;
  copyCommand(command: string): void;
  goBack(): void;
}

export interface WizardHistoryState {
  historyOpen: boolean;
  historyLoading: boolean;
  allPlans: MigrationPlan[];
  planSelection: Record<string, boolean>;
  deleteGateway: boolean;
  bulkReverting: boolean;
  bulkResult: BulkRevertResult | null;
  driftResults: Record<string, DriftEntry[]>;
  driftLoading: Record<string, boolean>;
  allSelected: boolean;
  selectedPlanIds: string[];
}

export interface WizardHistoryActions {
  toggleHistory(): void;
  toggleSelectAll(): void;
  togglePlanSelection(planId: string): void;
  confirmBulkRevert(): void;
  checkDrift(planId: string): void;
  setDeleteGateway(value: boolean): void;
}
