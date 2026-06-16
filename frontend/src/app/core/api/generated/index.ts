/**
 * OpenAPI-generated types — regenerate with `npm run generate:api` from repo root scripts.
 * Facades import from here; `../models/` re-exports aliases for feature code.
 */
import type { components } from './schema';

export type { components, operations, paths } from './schema';

export type MigrationPlanDto = components['schemas']['MigrationPlan'];
export type MigrationPrerequisiteDto = components['schemas']['MigrationPrerequisite'];
export type GeneratedResourceDto = components['schemas']['GeneratedResource'];
export type DriftEntryDto = components['schemas']['DriftEntry'];
export type ResourceResultDto = components['schemas']['ResourceResult'];
export type ApplyResultDto = components['schemas']['ApplyResult'];
export type BulkRevertResultDto = components['schemas']['BulkRevertResult'];
export type TestCommandDto = components['schemas']['TestCommand'];
export type ImportExportResponseDto = components['schemas']['ImportExportResponse'];

export type TargetClusterDto = components['schemas']['TargetCluster'];
export type ClusterReadinessDto = components['schemas']['ClusterReadiness'];
export type ClusterFeaturesDto = components['schemas']['ClusterFeatures'];
export type ProjectInfoDto = components['schemas']['ProjectInfo'];

export type ThreeScaleProductDto = components['schemas']['ThreeScaleProduct'];
export type ThreeScaleBackendDto = components['schemas']['ThreeScaleBackend'];
export type ThreeScaleAdminStatusDto = components['schemas']['ThreeScaleAdminStatus'];
export type ThreeScaleRefreshResultDto = components['schemas']['ThreeScaleRefreshResult'];
export type ThreeScaleSourceDto = components['schemas']['ThreeScaleSource'];
export type ThreeScaleSourceStatusDto = components['schemas']['ThreeScaleSourceStatus'];
