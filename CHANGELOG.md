# Changelog

All notable changes to GateForge are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.2.0]: https://github.com/Everything-is-Code/gateforge/compare/v0.1.9...v0.2.0
[0.1.9]: https://github.com/Everything-is-Code/gateforge/releases/tag/v0.1.9
