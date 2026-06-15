# Contributing to GateForge

Thank you for your interest in contributing.

## Language

Follow the RHCL program **[Language policy](https://github.com/Everything-is-Code/rhcl-ai/blob/main/AGENTS.md#language-policy)** (English only for code, docs, commits, PRs, and issues).

## Reporting issues

- Use the project’s GitHub **Issues** tab to report bugs, request features, or ask questions.
- Include steps to reproduce, expected vs. actual behavior, and versions (GateForge, OpenShift, 3scale, etc.) when reporting bugs.
- Search existing issues before opening a new one to avoid duplicates.

## How to contribute

Follow the RHCL program **[Git conventions and PR workflow](https://github.com/Everything-is-Code/rhcl-ai/blob/main/AGENTS.md#git-conventions)** (feature branches from `main`, PR required, English commit messages). GateForge specifics:

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

## Continuous integration

- **Backend unit tests** run on every pull request and push to `main` via the [Backend tests](.github/workflows/backend-tests.yml) workflow (`mvn test` in `backend/`).
- Reproduce locally: `cd backend && mvn test` (Java 17).
- `backend/Dockerfile.jvm` uses `-DskipTests` during image builds for speed; CI is the validation path for tests.
- Maintainers: consider marking **Backend tests / backend-test** as a required status check on `main` after this workflow is merged.

**Fastest path (containers):**

```bash
cp .env.example .env   # add 3scale token + optional AI key
./scripts/local-up.sh
```

No Kuadrant or OpenShift cluster is required for 3scale Admin API discovery and migration analysis.

## Code of Conduct

This project follows the [Contributor Covenant](https://www.contributor-covenant.org/) Code of Conduct. By participating, you are expected to uphold a respectful, inclusive environment for everyone.
