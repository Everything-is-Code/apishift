#!/usr/bin/env bash
# Backward-compatible wrapper — source scripts/lib/helm-repo-url.sh instead.
# shellcheck source=scripts/lib/helm-repo-url.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/helm-repo-url.sh"
