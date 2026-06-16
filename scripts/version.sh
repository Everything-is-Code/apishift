#!/usr/bin/env bash
# Sourceable: exports VERSION (semver) and VERSION_V (v-prefixed) from helm/gateforge/Chart.yaml.
set -euo pipefail
_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
_CHART="${_ROOT}/helm/gateforge/Chart.yaml"
if [[ ! -f "${_CHART}" ]]; then
  echo "Chart not found: ${_CHART}" >&2
  exit 1
fi
VERSION="$(grep '^version:' "${_CHART}" | awk '{print $2}' | tr -d '"')"
if [[ -z "${VERSION}" ]]; then
  echo "Could not read version from ${_CHART}" >&2
  exit 1
fi
export VERSION VERSION_V="v${VERSION}"
