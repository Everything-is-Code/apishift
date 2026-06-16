export type {
  ThreeScaleAdminStatusDto as ThreeScaleStatus,
  ThreeScaleBackendDto as ThreeScaleBackend,
  ThreeScaleRefreshResultDto as ThreeScaleRefreshResult,
  ThreeScaleSourceStatusDto as ThreeScaleSourceStatus,
} from '../generated';

export interface ThreeScaleProduct {
  name: string;
  namespace: string;
  systemName: string;
  description: string;
  deploymentOption: string;
  mappingRules: { httpMethod: string; pattern: string; metricRef: string; delta: number }[];
  backendUsages: { backendName: string; path: string }[];
  authentication: Record<string, unknown>;
  source: string;
  backendNamespace?: string;
  backendServiceName?: string;
  sourceCluster?: string;
}

export interface ThreeScaleSource {
  id: string;
  label: string;
  adminUrl: string;
  accessToken: string;
  enabled: boolean;
}
