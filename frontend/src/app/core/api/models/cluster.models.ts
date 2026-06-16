import { MigrationPrerequisite } from './migration.models';

export type { ClusterFeaturesDto as FeatureFlags } from '../generated';

export interface ProjectInfo {
  name: string;
  status: string;
  creationTimestamp: string;
  hasThreeScale: boolean;
  hasKuadrant: boolean;
}

export interface TargetCluster {
  id: string;
  label: string;
  apiServerUrl: string;
  token: string;
  authType: string;
  verifySsl: boolean;
  enabled: boolean;
}

export interface ClusterReadiness {
  clusterConnected: boolean;
  targetClusterId: string;
  connectionStatus: string;
  prerequisites: MigrationPrerequisite[];
}
