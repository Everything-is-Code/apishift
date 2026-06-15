#!/usr/bin/env bash
# Verify committed export-minimal tarball matches its SHA256 sidecar.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURES="${ROOT}/backend/src/test/resources/fixtures"
ARTIFACT="export-minimal-1.0.tar.gz"
TARBALL="${FIXTURES}/${ARTIFACT}"
CHECKSUM="${FIXTURES}/${ARTIFACT}.sha256"

if [[ ! -f "${TARBALL}" || ! -f "${CHECKSUM}" ]]; then
	echo "missing fixture tarball or checksum under ${FIXTURES}" >&2
	exit 1
fi

( cd "${FIXTURES}" && sha256sum -c "${ARTIFACT}.sha256" )
echo "export-minimal fixture checksum OK"
