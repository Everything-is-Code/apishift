import { MigrationPrerequisite } from './migration.models';

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

export interface FeatureFlags {
  developerHub: {
    enabled: boolean;
    url: string;
  };
}

export interface ClusterReadiness {
  clusterConnected: boolean;
  targetClusterId: string;
  connectionStatus: string;
  prerequisites: MigrationPrerequisite[];
}
