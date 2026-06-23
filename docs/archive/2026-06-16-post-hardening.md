# Archive: ApiShift Post-Hardening

**Change:** `ApiShift-post-hardening`  
**Archived:** 2026-06-16  
**SDD artifacts:** Engram `sdd/ApiShift-post-hardening/*`  
**Status:** Complete (Waves A–E)

## Intent

After architecture hardening Phases 1–4, close remaining maintainability debt: documentation drift, Helm/env confusion, unused OpenAPI typegen, large backend god classes, thin integration tests, and monolithic migration wizard orchestration — without changing product behavior.

## Delivery summary

| Wave | Focus | PRs merged | Issues |
|------|--------|------------|--------|
| A — docs | ARCHITECTURE, Helm env mapping, CHANGELOG, proxy, CONTRIBUTING | [#74](https://github.com/Everything-is-Code/ApiShift/pull/74), [#76](https://github.com/Everything-is-Code/ApiShift/pull/76), [#78](https://github.com/Everything-is-Code/ApiShift/pull/78) | [#73](https://github.com/Everything-is-Code/ApiShift/issues/73)–[#77](https://github.com/Everything-is-Code/ApiShift/issues/77) |
| B — contracts | Typed OpenAPI DTOs, typegen CI drift, facade migration | [#80](https://github.com/Everything-is-Code/ApiShift/pull/80), [#82](https://github.com/Everything-is-Code/ApiShift/pull/82), [#84](https://github.com/Everything-is-Code/ApiShift/pull/84) | [#79](https://github.com/Everything-is-Code/ApiShift/issues/79)–[#83](https://github.com/Everything-is-Code/ApiShift/issues/83) |
| C — backend | Apply service, endpoint resolver, OAS synthesis, warnings persistence | [#86](https://github.com/Everything-is-Code/ApiShift/pull/86), [#88](https://github.com/Everything-is-Code/ApiShift/pull/88), [#90](https://github.com/Everything-is-Code/ApiShift/pull/90), [#92](https://github.com/Everything-is-Code/ApiShift/pull/92) | [#85](https://github.com/Everything-is-Code/ApiShift/issues/85)–[#91](https://github.com/Everything-is-Code/ApiShift/issues/91) |
| D — tests | `@QuarkusTest` migration lifecycle + generator unit tests | [#94](https://github.com/Everything-is-Code/ApiShift/pull/94) | [#93](https://github.com/Everything-is-Code/ApiShift/issues/93) |
| E — frontend | Wizard state service, `shared/ui/` components, feature tests | [#96](https://github.com/Everything-is-Code/ApiShift/pull/96) | [#95](https://github.com/Everything-is-Code/ApiShift/issues/95) |

## Outcomes by wave

### Wave A — documentation

- `docs/ARCHITECTURE.md`: API contract workflow, completed hardening summary, port/adapter notes.
- `helm/ApiShift/README.md`: env mapping table, unwired values labeled, accurate defaults.
- `CHANGELOG.md`, `CONTRIBUTING.md`, `scripts/README.md`, frontend READMEs updated.
- `values.schema.json` examples aligned with `ThreeScaleSource` field names.

### Wave B — contracts

- Backend `@Schema` types: `DriftEntry`, `TestCommand`, `ClusterFeatures`, 3scale status types.
- `npm run generate:api` + CI drift check for `schema.ts` / OpenAPI artifacts.
- Core facades migrated to `core/api/generated/` types.

### Wave C — backend decomposition

- `ClusterResourceApplyService` — K8s apply/delete/drift extracted from REST layer.
- `BackendEndpointResolver` — 3scale backend endpoint indexing.
- `OpenApiSynthesisService` — OAS fetch + synthetic spec generation.
- Flyway V4 `consolidation_warnings_json`; repository round-trip; dead mappers removed.
- `MigrationService` reduced from ~1,350 → ~970 LOC.

### Wave D — test harness

- `@QuarkusTest`: import-export, apply, drift, revert lifecycle.
- `MigrationKubernetesTestSupport` Mockito K8s stubs.
- Generator unit tests: `GatewayResourceGenerator`, `HttpRouteResourceGenerator`.
- Backend: **112** tests green.

### Wave E — frontend decomposition

- `MigrationWizardStateService` + `migration-wizard.helpers.ts` (pure functions).
- `shared/ui/`: `LoadingSkeletonComponent`, `StatusBadgeComponent`, `BusyOverlayComponent`.
- `MigrationWizardComponent` slim shell (~110 LOC).
- Frontend: **44** tests green.

## Spec scope vs delivery

| Original proposal scope | Delivered |
|-------------------------|-----------|
| Waves A–B only in first SDD cycle | Waves A–E delivered in stacked PRs |
| Waves C–E deferred | Completed in same initiative per exploration recommendation |

## Verification

- Backend: `mvn test` — 112 tests.
- Frontend: `npm test` — 44 tests.
- CI: backend + frontend workflows green on all post-hardening PRs.

## Release

Shipped as **[v0.3.0](https://github.com/Everything-is-Code/ApiShift/releases/tag/v0.3.0)** on 2026-06-16. Full release audit trail: [docs/archive/2026-06-16-release-v0.3.0.md](2026-06-16-release-v0.3.0.md).

## Follow-up (post-archive, optional)

- Further `ThreeScaleService` decomposition (~763 LOC monolith remains).
- Adopt `shared/ui/` in dashboard, explorer, audit (skeleton/badge reuse).
- Explorer/dashboard component test coverage.
- Tighten any remaining loose OpenAPI schemas as endpoints evolve.

## SDD traceability (Engram)

| Artifact | Observation |
|----------|-------------|
| explore | #282 `sdd/explore/ApiShift-post-hardening-maintainability` |
| proposal | #283 `sdd/ApiShift-post-hardening/proposal` |
| spec | #284 `sdd/ApiShift-post-hardening/spec` |
| tasks (final) | #285 `sdd/ApiShift-post-hardening/tasks` |
| archive-report | `sdd/ApiShift-post-hardening/archive-report` |
