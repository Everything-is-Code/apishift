# Archive: GateForge Architecture Hardening

**Change:** `gateforge-architecture-hardening`  
**Archived:** 2026-06-16  
**SDD artifacts:** Engram `sdd/gateforge-architecture-hardening/*`  
**Status:** Complete (Phases 1–4)

## Intent

Improve long-term maintainability after team takeover without rewriting working product behavior (v0.2.0). Replicate the `service/export/` subdomain pattern across scripts, backend layering, frontend features, and API contracts.

## Delivery summary

| Phase | Focus | PRs merged | Issues |
|-------|--------|------------|--------|
| 1 — project structure | Script taxonomy, `ARCHITECTURE.md`, docs | [#50](https://github.com/Everything-is-Code/gateforge/pull/50), [#52](https://github.com/Everything-is-Code/gateforge/pull/52) | [#49](https://github.com/Everything-is-Code/gateforge/issues/49), [#51](https://github.com/Everything-is-Code/gateforge/issues/51) |
| 2 — backend layering | DevHub client, generators, repos, ExceptionMapper, REST smoke | [#54](https://github.com/Everything-is-Code/gateforge/pull/54)–[#60](https://github.com/Everything-is-Code/gateforge/pull/60) | [#53](https://github.com/Everything-is-Code/gateforge/issues/53)–[#59](https://github.com/Everything-is-Code/gateforge/issues/59) |
| 3 — frontend modules | `core/shared/features`, API facades, wizard steps | [#62](https://github.com/Everything-is-Code/gateforge/pull/62), [#64](https://github.com/Everything-is-Code/gateforge/pull/64), [#66](https://github.com/Everything-is-Code/gateforge/pull/66) | [#61](https://github.com/Everything-is-Code/gateforge/issues/61)–[#65](https://github.com/Everything-is-Code/gateforge/issues/65) |
| 4 — contracts & E2E | 3scale port, OpenAPI typegen, optional E2E workflow | [#68](https://github.com/Everything-is-Code/gateforge/pull/68), [#70](https://github.com/Everything-is-Code/gateforge/pull/70), [#72](https://github.com/Everything-is-Code/gateforge/pull/72) | [#67](https://github.com/Everything-is-Code/gateforge/issues/67), [#69](https://github.com/Everything-is-Code/gateforge/issues/69), [#71](https://github.com/Everything-is-Code/gateforge/issues/71) |

## Outcomes by capability

### project-structure (Phase 1)

- Scripts organized under `scripts/{lib,dev,ci,e2e,fixtures,release,docs}/` with `scripts/README.md` index.
- **Decision:** No backward-compatible top-level wrappers — canonical paths only (see Engram decision #273).
- `docs/ARCHITECTURE.md` created; linked from README and CONTRIBUTING.
- `frontend/README.md` rewritten for Angular 19; `sync-versions.sh` updates `frontend/package.json`.

### backend-layering (Phase 2)

- `DeveloperHubClient` extracted from `MigrationResource`.
- `service/generator/*` strategy implementations for Kuadrant resources.
- `MigrationPlanRepository`, `AuditRepository` for Panache access.
- `MigrationExceptionMapper` for structured REST errors.
- `@QuarkusTest` smoke for `/api/migration/analyze` with export-minimal fixture.

### frontend-feature-modules (Phase 3)

- Layout: `core/api/`, `shared/`, `features/*`.
- Domain facades: `MigrationApiService`, `ThreeScaleApiService`, `ClusterApiService`.
- Migration wizard split into `features/migration/steps/`; container ~730 LOC.

### Phase 4 (added during delivery)

- `ThreeScaleAdminPort` outbound port; `ThreeScaleAdminApiClient` as HTTP adapter.
- OpenAPI export (`backend/openapi/`), `npm run generate:api` → `frontend/src/app/core/api/generated/schema.ts`.
- Optional E2E: `.github/workflows/e2e-fixture.yml` (`workflow_dispatch`, default `fixture`).

## Spec deviations

| Original spec | Actual | Reason |
|---------------|--------|--------|
| Backward-compatible script wrappers for one release | Wrappers removed | User decision: canonical paths only (#273) |
| Phase 4 deferred indefinitely | Delivered in same initiative | Team continued stacked PRs after Phase 3 |

## Verification

- Backend: `mvn test` — 90 tests green after Phase 4 (includes `OpenApiBuildTest`).
- Frontend: `npm test` in CI on every PR.
- CI: OpenAPI export step in `backend-tests.yml`; E2E not on PR path (manual workflow only).

## Follow-up (post-archive, optional)

- Migrate hand-written `core/api/models/` to OpenAPI-generated types incrementally.
- Further reduce `MigrationService` LOC as new policy kinds are added.
- Run **E2E (optional)** workflow after releases or large migration changes.

## SDD traceability (Engram)

| Artifact | Observation |
|----------|-------------|
| proposal | #269 |
| spec | #270 |
| tasks (final) | `sdd/gateforge-architecture-hardening/tasks` (updated on archive) |
| archive-report | `sdd/gateforge-architecture-hardening/archive-report` |
