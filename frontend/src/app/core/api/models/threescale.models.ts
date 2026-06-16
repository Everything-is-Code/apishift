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

export interface ThreeScaleBackend {
  name: string;
  namespace?: string;
  id?: number;
  systemName?: string;
  privateEndpoint?: string;
  description?: string;
  source: string;
  createdAt?: string;
  updatedAt?: string;
  spec?: Record<string, unknown>;
}

export interface ThreeScaleStatus {
  configured: boolean;
  crdDiscoveryEnabled: boolean;
  reachable?: boolean;
  productCount?: number;
  backendApiCount?: number;
  activeDocsCount?: number;
  error?: string;
}

export interface ThreeScaleRefreshResult {
  productCount: number;
  backendCount: number;
  refreshedAt: string;
}
