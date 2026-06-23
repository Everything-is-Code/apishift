# ApiShift scripts

Automation for local development, CI, E2E, fixtures, releases, and docs screenshots.

## Layout

| Directory | Purpose |
|-----------|---------|
| `lib/` | Sourceable helpers (`common.sh`, `version.sh`, `helm-repo-url.sh`) — do not execute directly |
| `dev/` | Local Podman Compose stack |
| `ci/` | CI verification helpers |
| `e2e/` | End-to-end lab pipelines |
| `fixtures/` | Test fixture sync from 3scaleextract |
| `release/` | Version propagation across artifacts |
| `docs/` | Playwright screenshot capture (`npm ci` from this directory) |

## Scripts

| Script | Category | Purpose | Prerequisites | Example |
|--------|----------|---------|---------------|---------|
| `lib/common.sh` | lib | `ROOT`, `die()`, `log()` for other scripts | bash | `source scripts/lib/common.sh` |
| `lib/version.sh` | lib | Read semver from `helm/ApiShift/Chart.yaml` | bash | `source scripts/lib/version.sh && echo $VERSION` |
| `lib/helm-repo-url.sh` | lib | Export `HELM_REPO_URL` for GitHub Pages | bash | `source scripts/lib/helm-repo-url.sh` |
| `dev/local-up.sh` | dev | Build and start stack via podman-compose | Podman, podman-compose, `.env` | `./scripts/dev/local-up.sh` |
| `dev/local-down.sh` | dev | Stop local stack | podman-compose | `./scripts/dev/local-down.sh` |
| `ci/verify-export-minimal-fixture.sh` | ci | Verify export-minimal tarball SHA256 | bash, sha256sum | `./scripts/ci/verify-export-minimal-fixture.sh` |
| `ci/retry.sh` | ci | Retry flaky commands (CI resilience) | bash | `RETRY_ATTEMPTS=3 ./scripts/ci/retry.sh mvn test` |
| `ci/pr_preflight.sh` | ci | Unified PR validation (backend + OpenAPI + frontend) | Java 17+, Node 20+, Chrome | `./scripts/ci/pr_preflight.sh` |
| `ci/pipeline_health_report.py` | ci | Rolling GitHub Actions success-rate report | Python 3.12+, `gh` or `GH_TOKEN` | `python scripts/ci/pipeline_health_report.py --repo owner/repo` |
| `ci/export-openapi.sh` | ci | Emit OpenAPI schema via `OpenApiBuildTest` | Java 17+, Maven | `./scripts/ci/export-openapi.sh` |
| `ci/generate-frontend-api-types.sh` | ci | Sync schema to frontend and run `openapi-typescript` | Java 17+, Node 20+ | `./scripts/ci/generate-frontend-api-types.sh` |
| `e2e/seed-export-analyze.sh` | e2e | 3scaleextract seed → analyze via ApiShift API | curl, jq; optional Go/zip for live | `E2E_MODE=fixture ./scripts/e2e/seed-export-analyze.sh` |
| `fixtures/sync-export-minimal-fixture.sh` | fixtures | Copy export-minimal from 3scaleextract | 3scaleextract checkout | `THREESCALEEXTRACT_ROOT=../3scaleextract ./scripts/fixtures/sync-export-minimal-fixture.sh` |
| `release/sync-versions.sh` | release | Propagate Chart.yaml version to Maven, npm, Helm, docs | bash, sed | `./scripts/release/sync-versions.sh` |
| `docs/capture-screenshots.mjs` | docs | Capture UI PNGs for `docs/assets/screenshots/` | Node 20+, Playwright, Podman stack | `cd scripts/docs && npm ci && npm run capture-screenshots` |

See `lib/README.md` for sourceable helper details.

## E2E environment variables

Used by `e2e/seed-export-analyze.sh` (see script header for full usage):

| Variable | Default | Purpose |
|----------|---------|---------|
| `E2E_MODE` | `auto` | `fixture` \| `offline` \| `live` \| `auto` — `fixture` uses export-minimal tarball (no live 3scale) |
| `E2E_SKIP_SEED` | — | Set to `1` to reuse existing export directory |
| `THREESCALEEXTRACT_ROOT` | `../3scaleextract` | Path to 3scaleextract checkout (required for offline/live) |
| `ApiShift_API_URL` | `http://localhost:8080/api` | ApiShift API base URL |
| `THREESCALE_OUTPUT_DIR` | `{extract}/export` | Export output directory |
| `THREESCALE_REPORT_DIR` | `{extract}/report` | Visualize report output |
| `E2E_ZIP_FILE` | `{output}/../threescale-export-e2e.zip` | Packaged zip for import-export API |

## OpenAPI script chain

`generate-frontend-api-types.sh` calls `export-openapi.sh` first, then copies `backend/openapi/openapi.yaml` to `frontend/openapi/ApiShift.openapi.yaml` and runs `openapi-typescript`. Equivalent: `npm run generate:api` from `frontend/`.

## CI workflows using scripts

| Workflow | Scripts invoked |
|----------|-----------------|
| [PR Validation](../.github/workflows/pr-check.yml) | `ci/pr_preflight.sh` (uses `retry.sh`, `verify-export-minimal-fixture.sh`) |
| [Pipeline Health](../.github/workflows/pipeline-health.yml) | `ci/pipeline_health_report.py` |
| [Security Advisory](../.github/workflows/security-advisory.yml) | — (dependency-review + CodeQL, advisory mode) |
| [Backend tests](../.github/workflows/backend-tests.yml) | Manual only; same checks as preflight backend stage |
| [Frontend tests](../.github/workflows/frontend-tests.yml) | Manual only; same checks as preflight frontend stage |
| [E2E (optional)](../.github/workflows/e2e-fixture.yml) | `dev/local-up.sh`, `e2e/seed-export-analyze.sh` |
| [Capture screenshots](../.github/workflows/capture-screenshots.yml) | `dev/local-up.sh`, `e2e/seed-export-analyze.sh`, `docs/capture-screenshots.mjs --skip-stack` |
| [Release](../.github/workflows/release.yml) | `lib/helm-repo-url.sh`, `release/sync-versions.sh` |
