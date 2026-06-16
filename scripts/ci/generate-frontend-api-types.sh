#!/usr/bin/env bash
# Sync backend OpenAPI schema to frontend and regenerate TypeScript types.
set -euo pipefail
# shellcheck source=scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../lib/common.sh"

step() { printf '==> %s\n' "$*"; }

"${ROOT}/scripts/ci/export-openapi.sh"

SRC="${ROOT}/backend/openapi/openapi.yaml"
if [[ ! -f "${SRC}" ]]; then
  SRC="${ROOT}/backend/openapi/openapi.json"
fi
DEST_DIR="${ROOT}/frontend/openapi"
mkdir -p "${DEST_DIR}"
cp "${SRC}" "${DEST_DIR}/gateforge.openapi.yaml"

step "Generating TypeScript types from ${DEST_DIR}/gateforge.openapi.yaml"
(
  cd "${ROOT}/frontend"
  npx --yes openapi-typescript@7 openapi/gateforge.openapi.yaml -o src/app/core/api/generated/schema.ts
)

step "Done: frontend/src/app/core/api/generated/schema.ts"
