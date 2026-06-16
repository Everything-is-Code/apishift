#!/usr/bin/env bash
# M3 E2E lab: 3scaleextract seed → export → visualize → GateForge analyze.
# Requires a reachable 3scale Admin API for seed/export; GateForge for analyze.
#
# Usage:
#   export THREESCALE_ADMIN_URL=... THREESCALE_ACCESS_TOKEN=...
#   ./scripts/e2e/seed-export-analyze.sh
#
# Optional:
#   E2E_MODE=offline|live|auto|fixture   (default: auto)
#   E2E_SKIP_SEED=1                      reuse existing export directory
#   THREESCALEEXTRACT_ROOT=../3scaleextract
#   GATEFORGE_API_URL=http://localhost:8080/api
#   THREESCALE_OUTPUT_DIR=./export
set -euo pipefail
# shellcheck source=scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../lib/common.sh"

step() { printf '==> %s\n' "$*"; }

EXTRACT_ROOT="${THREESCALEEXTRACT_ROOT:-}"
if [[ -z "${EXTRACT_ROOT}" ]]; then
	EXTRACT_ROOT="$(cd "${ROOT}/../3scaleextract" 2>/dev/null && pwd || true)"
fi
GATEFORGE_API="${GATEFORGE_API_URL:-http://localhost:8080/api}"
OUTPUT_DIR="${THREESCALE_OUTPUT_DIR:-${EXTRACT_ROOT}/export}"
REPORT_DIR="${THREESCALE_REPORT_DIR:-${EXTRACT_ROOT}/report}"
MODE="${E2E_MODE:-auto}"
ZIP_FILE="${E2E_ZIP_FILE:-${OUTPUT_DIR%/}/../threescale-export-e2e.zip}"

PRODUCT_NAMES=(
	"Seed API Key Product"
	"Seed OIDC Product"
	"Seed App ID Product"
	"Seed Multi-Backend Product"
)

require_cmd() {
	for cmd in "$@"; do
		command -v "$cmd" >/dev/null 2>&1 || die "missing required command: ${cmd}"
	done
}

load_extract_env() {
	if [[ -n "${EXTRACT_ROOT}" && -f "${EXTRACT_ROOT}/.env" ]]; then
		set -a
		# shellcheck source=/dev/null
		source "${EXTRACT_ROOT}/.env"
		set +a
	fi
}

gateforge_ready() {
	curl -sf "${GATEFORGE_API%/api}/q/health/ready" >/dev/null 2>&1
}

run_seed_export() {
	[[ -n "${EXTRACT_ROOT}" ]] || die "3scaleextract not found; set THREESCALEEXTRACT_ROOT"
	[[ -f "${EXTRACT_ROOT}/scripts/demo/seed-and-export.sh" ]] \
		|| die "missing ${EXTRACT_ROOT}/scripts/demo/seed-and-export.sh"

	: "${THREESCALE_ADMIN_URL:?set THREESCALE_ADMIN_URL}"
	: "${THREESCALE_ACCESS_TOKEN:?set THREESCALE_ACCESS_TOKEN}"

	if [[ "${E2E_SKIP_SEED:-}" == "1" && -f "${OUTPUT_DIR}/manifest.json" ]]; then
		step "E2E_SKIP_SEED=1 — reusing ${OUTPUT_DIR}/manifest.json"
		return 0
	fi

	step "Seeding and exporting via 3scaleextract"
	(
		cd "${EXTRACT_ROOT}"
		THREESCALE_OUTPUT_DIR="${OUTPUT_DIR}" ./scripts/demo/seed-and-export.sh
	)
}

verify_export_manifest() {
	[[ -f "${OUTPUT_DIR}/manifest.json" ]] || die "missing ${OUTPUT_DIR}/manifest.json"
	local incomplete count
	incomplete="$(jq -r 'if .incomplete == true then "true" else "false" end' "${OUTPUT_DIR}/manifest.json")"
	count="$(jq -r '.product_count // 0' "${OUTPUT_DIR}/manifest.json")"
	[[ "${incomplete}" == "false" ]] || die "export incomplete=true (check Admin token and tenant)"
	[[ "${count}" -ge 4 ]] || die "expected product_count>=4, got ${count}"
	step "Export manifest OK (${count} products, incomplete=false)"
}

run_visualize() {
	[[ -n "${EXTRACT_ROOT}" ]] || die "3scaleextract required for visualize"
	step "Building threescale-visualize"
	(
		cd "${EXTRACT_ROOT}"
		go build -o bin/threescale-visualize ./cmd/threescale-visualize
		rm -rf "${REPORT_DIR}"
		bin/threescale-visualize "${OUTPUT_DIR}" -o "${REPORT_DIR}"
	)
	[[ -f "${REPORT_DIR}/index.md" ]] || die "visualize report missing: ${REPORT_DIR}/index.md"
	step "Visualize report: ${REPORT_DIR}/index.md"
}

prepare_fixture_export() {
	local fixture_tar="${ROOT}/backend/src/test/resources/fixtures/export-minimal-1.0.tar.gz"
	[[ -f "${fixture_tar}" ]] || die "missing fixture tarball: ${fixture_tar}"
	rm -rf "${OUTPUT_DIR}"
	mkdir -p "$(dirname "${OUTPUT_DIR}")"
	tar -xzf "${fixture_tar}" -C "$(dirname "${OUTPUT_DIR}")"
	if [[ -d "$(dirname "${OUTPUT_DIR}")/export-minimal" ]]; then
		mv "$(dirname "${OUTPUT_DIR}")/export-minimal" "${OUTPUT_DIR}"
	fi
	[[ -f "${OUTPUT_DIR}/manifest.json" ]] || die "fixture extract missing manifest.json"
	PRODUCT_NAMES=("Seed Alpha Product" "Seed Multi-Backend Product")
	step "Fixture export loaded (smoke analyze, ${#PRODUCT_NAMES[@]} products)"
}

zip_export() {
	require_cmd zip
	rm -f "${ZIP_FILE}"
	(
		cd "${OUTPUT_DIR}"
		zip -qr "${ZIP_FILE}" .
	)
	[[ -f "${ZIP_FILE}" ]] || die "failed to create ${ZIP_FILE}"
	step "Packaged export zip: ${ZIP_FILE}"
}

import_export_offline() {
	zip_export
	step "POST ${GATEFORGE_API}/migration/import-export"
	local response
	response="$(curl -sf -X POST "${GATEFORGE_API}/migration/import-export" -F "file=@${ZIP_FILE}")"
	echo "${response}" | jq -e '.importMode == "export-v1"' >/dev/null
	local imported
	imported="$(echo "${response}" | jq -r '.productCount')"
	step "Imported ${imported} product(s) (offline)"
}

refresh_live_products() {
	step "POST ${GATEFORGE_API}/threescale/refresh"
	curl -sf -X POST "${GATEFORGE_API}/threescale/refresh" -H 'Content-Type: application/json' -d '{}' >/dev/null
}

analyze_products() {
	local products_json
	products_json="$(printf '%s\n' "${PRODUCT_NAMES[@]}" | jq -R . | jq -s .)"
	step "POST ${GATEFORGE_API}/migration/analyze (${#PRODUCT_NAMES[@]} products)"
	local plan
	plan="$(curl -sf -X POST "${GATEFORGE_API}/migration/analyze" \
		-H 'Content-Type: application/json' \
		-d "{\"gatewayStrategy\":\"shared\",\"products\":${products_json},\"targetClusterId\":\"local\"}")"

	local plan_id auth_count jwt_count apikey_count httproute_count warnings
	plan_id="$(echo "${plan}" | jq -r '.id')"
	auth_count="$(echo "${plan}" | jq '[.resources[] | select(.kind == "AuthPolicy")] | length')"
	jwt_count="$(echo "${plan}" | jq '[.resources[] | select(.kind == "AuthPolicy" and (.yaml | contains("issuerUrl")))] | length')"
	apikey_count="$(echo "${plan}" | jq '[.resources[] | select(.kind == "AuthPolicy" and (.yaml | contains("apiKey")))] | length')"
	httproute_count="$(echo "${plan}" | jq '[.resources[] | select(.kind == "HTTPRoute")] | length')"
	warnings="$(echo "${plan}" | jq -r '.consolidationWarnings // [] | join("\n")')"

	[[ -n "${plan_id}" && "${plan_id}" != "null" ]] || die "analyze returned no plan id"
	[[ "${auth_count}" -ge 1 ]] || die "expected at least one AuthPolicy, got ${auth_count}"
	[[ "${httproute_count}" -ge 1 ]] || die "expected at least one HTTPRoute, got ${httproute_count}"

	step "Plan ${plan_id}: AuthPolicy=${auth_count}, HTTPRoute=${httproute_count}"

	if [[ "${#PRODUCT_NAMES[@]}" -ge 4 ]]; then
		[[ "${auth_count}" -ge 4 ]] || die "expected >=4 AuthPolicy for lab fixtures, got ${auth_count}"
		[[ "${jwt_count}" -ge 1 ]] || die "expected OIDC AuthPolicy (issuerUrl), got ${jwt_count}"
		[[ "${apikey_count}" -ge 1 ]] || die "expected API key AuthPolicy (apiKey), got ${apikey_count}"
	fi

	if [[ "${warnings}" == *"OIDC"* || "${warnings}" == *"oidc"* || "${warnings}" == *"issuer"* ]]; then
		step "OIDC-related consolidation warning present (expected for lab placeholder issuer)"
	else
		step "No OIDC consolidation warning (OK if issuer resolved from export)"
	fi

	echo "${plan}" | jq '{id, gatewayStrategy, sourceProducts, resourceKinds: [.resources[].kind] | unique, warnings: .consolidationWarnings}'
	step "E2E analyze checks passed"
}

resolve_mode() {
	case "${MODE}" in
		fixture) echo "fixture" ;;
		offline) echo "offline" ;;
		live) echo "live" ;;
		auto)
			if gateforge_ready; then
				echo "offline"
			else
				die "GateForge not ready at ${GATEFORGE_API}; start ./scripts/dev/local-up.sh or set E2E_MODE=fixture"
			fi
			;;
		*) die "unknown E2E_MODE=${MODE} (use auto|offline|live|fixture)" ;;
	esac
}

main() {
	require_cmd curl jq
	load_extract_env

	local resolved
	resolved="$(resolve_mode)"
	step "E2E mode: ${resolved}"

	case "${resolved}" in
		fixture)
			prepare_fixture_export
			gateforge_ready || die "GateForge required for analyze; start ./scripts/dev/local-up.sh"
			import_export_offline
			;;
		*)
			[[ -n "${EXTRACT_ROOT}" ]] || die "set THREESCALEEXTRACT_ROOT to 3scaleextract checkout"
			require_cmd go zip
			run_seed_export
			verify_export_manifest
			run_visualize
			gateforge_ready || die "GateForge not ready at ${GATEFORGE_API}"
			if [[ "${resolved}" == "live" ]]; then
				refresh_live_products
			else
				import_export_offline
			fi
			;;
	esac

	analyze_products
	step "E2E lab pipeline complete"
}

main "$@"
