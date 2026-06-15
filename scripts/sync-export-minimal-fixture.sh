#!/usr/bin/env bash
# Sync export-minimal fixture tarball from a local 3scaleextract checkout.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_REPO="${3SCALEEXTRACT_ROOT:-$(cd "${ROOT}/../3scaleextract" 2>/dev/null && pwd || true)}"
DEST="${ROOT}/backend/src/test/resources/fixtures"
ARTIFACT="export-minimal-1.0.tar.gz"

if [[ -z "${SOURCE_REPO}" || ! -f "${SOURCE_REPO}/testdata/${ARTIFACT}" ]]; then
	echo "3scaleextract testdata not found. Set 3SCALEEXTRACT_ROOT or clone beside gateforge." >&2
	exit 1
fi

mkdir -p "${DEST}"
cp "${SOURCE_REPO}/testdata/${ARTIFACT}" "${SOURCE_REPO}/testdata/${ARTIFACT}.sha256" "${DEST}/"
echo "synced ${DEST}/${ARTIFACT}"
