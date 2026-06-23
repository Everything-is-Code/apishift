# Changelog

All notable changes to ApiShift are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security

- **API secret redaction**: `GET`/`POST` responses for `/api/threescale/sources` and `/api/cluster/targets` no longer include `accessToken` or `token`; responses expose `credentialConfigured` instead. POST request bodies still accept credentials for create.

### Changed

- **Settings**: source and cluster lists show a credential status badge derived from `credentialConfigured` (tokens are never displayed after save).

## [0.3.0] - 2026-06-16

### Added

- **Architecture hardening (Phases 1–4)**: script taxonomy, `docs/ARCHITECTURE.md`, backend layering (generators, repositories, `DeveloperHubClient`, `MigrationExceptionMapper`), frontend `core/shared/features`, API facades, wizard step components, `ThreeScaleAdminPort`, OpenAPI typegen, optional E2E workflow
- **Post-hardening Wave A**: API contract workflow in ARCHITECTURE.md, Helm env mapping table with unwired-value disclosure, REST change checklist in CONTRIBUTING.md, E2E env vars and CI index in `scripts/README.md`, frontend dev proxy docs
- **Post-hardening Wave B**: typed OpenAPI schemas (`DriftEntry`, `TestCommand`, `ClusterFeatures`, 3scale status types), `npm run generate:api`, CI drift check, facade migration to `core/api/generated/`
- **Post-hardening Wave C**: `ClusterResourceApplyService`, `BackendEndpointResolver`, `OpenApiSynthesisService`; Flyway V4 `consolidation_warnings_json` persistence
- **Post-hardening Wave D**: `@QuarkusTest` migration import-export, apply, drift, revert; generator unit tests (`GatewayResourceGenerator`, `HttpRouteResourceGenerator`)
- **Post-hardening Wave E**: `MigrationWizardStateService`, `shared/ui/` components (`LoadingSkeleton`, `StatusBadge`, `BusyOverlay`)
- **Archive docs**: `docs/archive/2026-06-16-architecture-hardening.md`, `docs/archive/2026-06-16-post-hardening.md`

### Changed

- **Backend**: `MigrationService` reduced (~1,350 → ~970 LOC); dead mappers and deprecated `ThreeScaleService.refreshApplications` removed
- **Frontend**: migration wizard orchestration extracted to state service; **44** unit tests (was 31)
- **Documentation**: Helm `values.schema.json` examples aligned with `ThreeScaleSource`; `rbac.clusterAdmin` default documented accurately
- **Tests**: backend suite expanded to **112** tests

## [0.2.0] - 2026-06-10

### Added

- **M2 offline import**: `POST /api/migration/import-export`, export zip upload in Migration Wizard, 3scaleextract fixture integration
- **M3 E2E lab**: `scripts/e2e/seed-export-analyze.sh` with `E2E_MODE` (`auto`, `live`, `offline`, `fixture`)
- **RHCL 1.3 policy mapping**: gap catalog, prerequisites panel, `GET /api/migration/policy-mapping`
- **Discovery refresh**: `POST /api/threescale/refresh` bypasses cache on demand
- **Release pipeline**: `release.yml` publishes immutable semver images and Helm chart on `v*` tags
- **Version sync**: `scripts/lib/version.sh` and `scripts/release/sync-versions.sh` (Chart.yaml as SSOT)
- **Local dev**: `podman-compose.yml`, `scripts/dev/local-up.sh` / `local-down.sh`
- **CI**: backend and frontend unit test workflows; export fixture verification script
- **Governance**: CODEOWNERS, CONTRIBUTING, org metadata

### Changed

- **CI images**: `main` builds push only `latest` and commit SHA tags (semver tags on release only)
- **Frontend footer**: version injected at Docker build time via `APP_VERSION`
- **GitHub Pages**: M3 E2E documentation, org URL (`everything-is-code.github.io`)
- **AI FAQ warm-up**: `AI_TIMEOUT`, retries, lighter context for slow models

### Fixed

- Live export parser: `backend_usages.json` array form, List-kind product YAML, API-key vs OIDC classification
- jq boolean handling in E2E manifest checks

## [0.1.9] - 2026-04-27

### Added

- APICast self-managed / multi-tenant discovery and migration
- 3scale entity conflict resolution
- ObservabilityTab and ComponentEditor fixes in DevHub plugin

[0.3.0]: https://github.com/Everything-is-Code/ApiShift/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Everything-is-Code/ApiShift/compare/v0.1.9...v0.2.0
[0.1.9]: https://github.com/Everything-is-Code/ApiShift/releases/tag/v0.1.9
