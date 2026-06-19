#!/usr/bin/env bash
# Unified PR validation: backend tests, OpenAPI contract, frontend tests.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

echo "Running PR preflight: export-minimal fixture verification."
./scripts/ci/verify-export-minimal-fixture.sh

echo "Running PR preflight: backend OpenAPI export and unit tests."
RETRY_ATTEMPTS="${RETRY_ATTEMPTS:-2}" RETRY_SLEEP_SECONDS="${RETRY_SLEEP_SECONDS:-20}" \
  ./scripts/ci/retry.sh bash -c '
    set -euo pipefail
    cd backend
    mvn -q -Dtest=OpenApiBuildTest test
    mvn verify
  '

echo "Running PR preflight: frontend OpenAPI types in sync."
(
  cd frontend
  npm run generate:api
)
git diff --exit-code -- \
  backend/openapi/ \
  frontend/openapi/ \
  frontend/src/app/core/api/generated/

echo "Running PR preflight: frontend unit tests."
RETRY_ATTEMPTS="${RETRY_ATTEMPTS:-2}" RETRY_SLEEP_SECONDS="${RETRY_SLEEP_SECONDS:-15}" \
  ./scripts/ci/retry.sh bash -c '
    set -euo pipefail
    cd frontend
    npm test -- --no-watch --browsers=ChromeHeadlessCI
  '

echo "PR preflight passed."
