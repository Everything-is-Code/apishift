#!/usr/bin/env bash
# Backward-compatible wrapper — source scripts/lib/version.sh instead.
# shellcheck source=scripts/lib/version.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/version.sh"
