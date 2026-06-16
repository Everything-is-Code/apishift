export interface GeneratedResource {
  kind: string;
  name: string;
  namespace: string;
  yaml: string;
}

export interface MigrationPrerequisite {
  id: string;
  category: string;
  title: string;
  description: string;
  requiredByPlan: boolean;
  optionalTier: boolean;
  docUrl?: string;
  status: 'satisfied' | 'missing' | 'unknown' | 'not_applicable';
  triggeredByCount: number;
}

export interface MigrationPlan {
  id: string;
  gatewayStrategy: string;
  sourceProducts: string[];
  resources: GeneratedResource[];
  aiAnalysis: string;
  createdAt: string;
  catalogInfoYaml?: string;
  status?: string;
  targetClusterId?: string;
  targetClusterLabel?: string;
  consolidationWarnings?: string[];
  prerequisites?: MigrationPrerequisite[];
}

export interface DriftEntry {
  kind: string;
  name: string;
  namespace: string;
  status: 'in-sync' | 'missing' | 'error';
  message?: string;
}

export interface ResourceResult {
  kind: string;
  name: string;
  namespace: string;
  success: boolean;
  message: string;
}

export interface ApplyResult {
  planId: string;
  applied: number;
  failed: number;
  results: ResourceResult[];
}

export interface BulkRevertResult {
  totalPlans: number;
  totalReverted: number;
  totalFailed: number;
  planResults: ApplyResult[];
}

export interface TestCommand {
  label: string;
  command: string;
  type: string;
}

export interface ImportExportResponse {
  importMode: string;
  productCount: number;
  products: { name: string; systemName: string; serviceId: number }[];
  manifest: {
    schemaVersion: string;
    adminUrl: string;
    exportedAt: string;
  };
}
