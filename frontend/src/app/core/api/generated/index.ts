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
