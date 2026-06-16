#!/usr/bin/env bash
# Start GateForge locally with Podman Compose.
set -euo pipefail
# shellcheck source=scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../lib/common.sh"
cd "${ROOT}"

if [[ ! -f .env ]]; then
	echo "Missing .env — copy the template and set your secrets:"
	echo "  cp .env.example .env"
	exit 1
fi

if ! command -v podman-compose >/dev/null 2>&1; then
	die "podman-compose is required. Install podman-compose or use: podman compose"
fi

echo "Building and starting GateForge (podman-compose)..."
podman-compose --env-file .env up -d --build "$@"

echo ""
echo "Waiting for backend health (up to ~2 min on first build)..."
for i in $(seq 1 40); do
	if curl -sf http://localhost:8080/q/health/ready >/dev/null 2>&1; then
		echo "Backend ready."
		break
	fi
	sleep 3
	if [[ "$i" -eq 40 ]]; then
		die "Backend not ready yet — check: podman logs gateforge-backend"
	fi
done

echo ""
echo "GateForge URLs:"
echo "  UI:      http://localhost:4200"
echo "  API:     http://localhost:8080/api"
echo "  Health:  http://localhost:8080/q/health/ready"
echo "  3scale:  http://localhost:8080/api/threescale/status"
echo ""
echo "Logs: podman-compose logs -f"
