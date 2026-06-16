#!/usr/bin/env bash
# Export OpenAPI schema from the Quarkus build (requires Java 17+).
# Writes backend/openapi/openapi.yaml and openapi.json when present.
set -euo pipefail
# shellcheck source=scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../lib/common.sh"

step() { printf '==> %s\n' "$*"; }

BACKEND_DIR="${ROOT}/backend"
OPENAPI_DIR="${BACKEND_DIR}/openapi"

step "Running OpenApiBuildTest to emit OpenAPI schema"
(
  cd "${BACKEND_DIR}"
  mvn -q -Dtest=OpenApiBuildTest test
)

if [[ ! -f "${OPENAPI_DIR}/openapi.yaml" && ! -f "${OPENAPI_DIR}/openapi.json" ]]; then
  die "OpenAPI schema not found under ${OPENAPI_DIR}/ — check quarkus.smallrye-openapi.store-schema-directory"
fi

step "OpenAPI export OK: ${OPENAPI_DIR}"
