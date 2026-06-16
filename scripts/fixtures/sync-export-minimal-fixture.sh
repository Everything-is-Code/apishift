#!/usr/bin/env bash
# Sync export-minimal fixture tarball from a local 3scaleextract checkout.
set -euo pipefail
# shellcheck source=scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../lib/common.sh"
SOURCE_REPO="${THREESCALEEXTRACT_ROOT:-}"
if [[ -z "${SOURCE_REPO}" ]]; then
	SOURCE_REPO="$(cd "${ROOT}/../3scaleextract" 2>/dev/null && pwd || true)"
fi
DEST="${ROOT}/backend/src/test/resources/fixtures"
ARTIFACT="export-minimal-1.0.tar.gz"

if [[ -z "${SOURCE_REPO}" || ! -f "${SOURCE_REPO}/testdata/${ARTIFACT}" ]]; then
	die "3scaleextract testdata not found. Set THREESCALEEXTRACT_ROOT or clone beside gateforge."
fi

mkdir -p "${DEST}"
cp "${SOURCE_REPO}/testdata/${ARTIFACT}" "${SOURCE_REPO}/testdata/${ARTIFACT}.sha256" "${DEST}/"
echo "synced ${DEST}/${ARTIFACT}"
