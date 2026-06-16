/**
 * OpenAPI-generated types — regenerate with `npm run generate:api` from repo root scripts.
 * Hand-written DTOs in `../models/` remain in use; migrate facades to these types incrementally.
 */
import type { components } from './schema';

export type { components, operations, paths } from './schema';

export type MigrationPlanDto = components['schemas']['MigrationPlan'];
export type ThreeScaleProductDto = components['schemas']['ThreeScaleProduct'];
export type TargetClusterDto = components['schemas']['TargetCluster'];
export type ClusterReadinessDto = components['schemas']['ClusterReadiness'];
export type MigrationPrerequisiteDto = components['schemas']['MigrationPrerequisite'];
export type DriftEntryDto = components['schemas']['DriftEntry'];
export type TestCommandDto = components['schemas']['TestCommand'];
export type ClusterFeaturesDto = components['schemas']['ClusterFeatures'];
export type ThreeScaleBackendDto = components['schemas']['ThreeScaleBackend'];
export type ThreeScaleAdminStatusDto = components['schemas']['ThreeScaleAdminStatus'];
export type ThreeScaleRefreshResultDto = components['schemas']['ThreeScaleRefreshResult'];
export type ThreeScaleSourceStatusDto = components['schemas']['ThreeScaleSourceStatus'];
