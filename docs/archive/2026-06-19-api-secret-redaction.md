# Archive: ApiShift API Secret Redaction

**Change:** `apishift-api-secret-redaction`  
**Archived:** 2026-06-19  
**SDD artifacts:** Engram `sdd/apishift-api-secret-redaction/*`  
**Status:** Complete  
**Issue:** [#103](https://github.com/Everything-is-Code/apishift/issues/103) (closed)

## Intent

`GET`/`POST` responses for `/api/threescale/sources` and `/api/cluster/targets` leaked stored `accessToken` and Kubernetes `token` in JSON. Redact credentials at the REST boundary; keep internal registries and POST create payloads unchanged.

## Delivery summary

| PR | Focus | Merged commit |
|----|--------|---------------|
| [#118](https://github.com/Everything-is-Code/apishift/pull/118) | View DTOs, REST mapping, `@QuarkusTest` | `4dad109` |
| [#119](https://github.com/Everything-is-Code/apishift/pull/119) | OpenAPI typegen, API facades | `5b54f3c` |
| [#120](https://github.com/Everything-is-Code/apishift/pull/120) | Settings badges, CHANGELOG | `883e50c` |

Stacked PRs merged to `main` with squash + `--admin` (PR #118 required admin bypass for interim OpenAPI drift until PR #119 landed).

## Outcomes

### Backend (PR #118)

- `ThreeScaleSourceView` / `TargetClusterView` with `credentialConfigured` (no secret fields).
- `ThreeScaleResource` and `ClusterResource` return Views on GET/POST list endpoints.
- `CredentialRedactionResourceTest` + `CredentialViewMapperTest`.
- Backend OpenAPI export references View schemas.
- Backend tests: **121** passed.

### Frontend contract (PR #119)

- `npm run generate:api` — `schema.ts`, `gateforge.openapi.yaml` aligned with View schemas.
- `ThreeScaleApiService` / `ClusterApiService` — View types on responses; create payloads retain secrets.
- Migration wizard and Settings list types migrated to View shapes.

### UI + docs (PR #120)

- Settings: **Credential set** / **No credential** badges from `credentialConfigured`.
- `CHANGELOG.md` Unreleased: Security + Changed entries for breaking GET response shape.
- Frontend: **44** tests passed; production build green.

## Verification

| Check | Result |
|-------|--------|
| `sdd-verify` PR1 | PASS (9/9 scenarios) |
| `mvn test` | 121 passed |
| `npm test` | 44 passed |
| CI on `main` (`883e50c`) | PR Validation, Quay push, Pages — all green |

## Spec scope vs delivery

| Requirement | Delivered |
|-------------|-----------|
| Redact `accessToken` / `token` from list responses | Yes |
| `credentialConfigured` on View DTOs | Yes |
| POST bodies still accept credentials | Yes |
| Hub topology/overview safe | Yes (unchanged + tested) |
| OpenAPI response schemas omit secrets | Yes |
| CHANGELOG breaking note | Yes |

## Backlog (next session)

Planned order after this initiative:

| Phase | Initiative | Issue |
|-------|------------|-------|
| F0 | Dependabot | [#101](https://github.com/Everything-is-Code/apishift/issues/101) |
| F0 | Coverage tooling (Jacoco + karma, 80%) | [#106](https://github.com/Everything-is-Code/apishift/issues/106) |
| F1 | Input safety (XSS, YAML) | [#105](https://github.com/Everything-is-Code/apishift/issues/105) |
| F1 | API authentication | [#102](https://github.com/Everything-is-Code/apishift/issues/102) |

Additional open issues: #104 (CORS), #107–#117 (testing, clean-code, observability, E2E).

## SDD traceability (Engram)

| Artifact | Observation |
|----------|-------------|
| explore | #386 `sdd/explore/apishift-api-secret-redaction` |
| proposal | #387 `sdd/apishift-api-secret-redaction/proposal` |
| spec | #388 `sdd/apishift-api-secret-redaction/spec` |
| tasks (final) | #389 `sdd/apishift-api-secret-redaction/tasks` |
| verify-report | #390 `sdd/apishift-api-secret-redaction/verify-report` |
| archive-report | `sdd/apishift-api-secret-redaction/archive-report` |
