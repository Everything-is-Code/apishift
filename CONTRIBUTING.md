# Contributing to ApiShift

Thank you for your interest in contributing.

## Language

Follow the RHCL program **[Language policy](https://github.com/Everything-is-Code/rhcl-ai/blob/main/AGENTS.md#language-policy)** (English only for code, docs, commits, PRs, and issues).

## Reporting issues

- Use the project’s GitHub **Issues** tab to report bugs, request features, or ask questions.
- Include steps to reproduce, expected vs. actual behavior, and versions (ApiShift, OpenShift, 3scale, etc.) when reporting bugs.
- Search existing issues before opening a new one to avoid duplicates.

## How to contribute

Follow the RHCL program **[Git conventions and PR workflow](https://github.com/Everything-is-Code/rhcl-ai/blob/main/AGENTS.md#git-conventions)** (feature branches from `main`, PR required, English commit messages). ApiShift specifics:

1. Branch names: `feature/GF-<issue>-short-desc` or `fix/GF-<issue>-short-desc` (link the GitHub issue when one exists).
2. Open a **pull request** using this repo’s [PR template](.github/pull_request_template.md); include a concrete test plan.
3. Address review feedback. A maintainer merges when approved (see [CODEOWNERS](.github/CODEOWNERS)).

For large changes (>400 lines), prefer **stacked PRs** (e.g. backend first, then frontend on the prior branch) so reviewers can focus.

## Development requirements

- **Java 17**
- **Node.js 20**
- **Maven** (3.9+ recommended)
- **Helm 3** (for chart development and deployment testing)

See the main [README](README.md) for how to run the backend, frontend, and local stack.

For module layout, REST surface, and migration data flows, read **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

## Continuous integration

- **Backend tests** — [Backend tests](.github/workflows/backend-tests.yml): `mvn test` in `backend/`, export-minimal fixture verify, OpenAPI export via `OpenApiBuildTest`.
- **Frontend tests** — [Frontend tests](.github/workflows/frontend-tests.yml): `npm ci` + `npm test` (ChromeHeadless) in `frontend/`.
- Reproduce locally: `cd backend && mvn test` (Java 17); `cd frontend && npm test` (Node 20).
- `backend/Dockerfile.jvm` uses `-DskipTests` during image builds for speed; CI is the validation path for tests.

**Optional E2E (slow):** [E2E (optional)](.github/workflows/e2e-fixture.yml) runs only on **workflow_dispatch** (Actions → E2E (optional) → Run workflow). Default mode is `fixture` (no live 3scale). Locally: `E2E_MODE=fixture ./scripts/e2e/seed-export-analyze.sh` with `./scripts/dev/local-up.sh` running.

### Dependency updates (Dependabot)

[Dependabot](.github/dependabot.yml) opens weekly PRs for Maven (`backend/`), npm (`frontend/`), and GitHub Actions. Each PR runs **PR Validation** (`pr-check.yml`) and the advisory **Security Advisory** workflow (dependency review). Review and merge like any other contribution; patch/minor auto-merge is out of scope for now (see [#101](https://github.com/Everything-is-Code/apishift/issues/101)).

### REST contract changes

When adding or changing a REST endpoint or response DTO:

1. Update Java types / OpenAPI annotations in `backend/`.
2. Run `npm run generate:api` from `frontend/` (or `./scripts/ci/generate-frontend-api-types.sh` from repo root).
3. Commit `backend/openapi/*`, `frontend/openapi/ApiShift.openapi.yaml`, and `frontend/src/app/core/api/generated/schema.ts`.
4. Migrate affected facades in `frontend/src/app/core/api/` to generated types when schemas are stable.
5. Ensure backend and frontend CI are green.

**Authentication checklist** (when `apishift.auth.enabled=true`):

- **Mutations** (`POST`, `PUT`, `DELETE`): add `@RolesAllowed` — `admin` for destructive/config changes, `{operator, admin}` for operational workflows (analyze, refresh).
- **Sensitive reads**: add path to `ReadProtectionAuthorizationFilter.isProtectedReadPath()` if the endpoint exposes plans, audit, hub, products, or targets data.
- **Public probes**: keep health, readiness, status, and policy-mapping endpoints unauthenticated; document new public paths in `docs/ARCHITECTURE.md`.
- **Tests**: extend `AuthEnforcementTest` or `AuthReadProtectionTest` for new 401/403 behavior; default test profile keeps auth off for regression suite.

**Fastest path (containers):**

```bash
cp .env.example .env   # add 3scale token + optional AI key
./scripts/dev/local-up.sh
```

No Kuadrant or OpenShift cluster is required for 3scale Admin API discovery and migration analysis.

## Code of Conduct

This project follows the [Contributor Covenant](https://www.contributor-covenant.org/) Code of Conduct. By participating, you are expected to uphold a respectful, inclusive environment for everyone.
