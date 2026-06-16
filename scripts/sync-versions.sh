#!/usr/bin/env bash
# Propagate helm/gateforge/Chart.yaml version to dependent artifacts.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/version.sh
source "${ROOT}/scripts/version.sh"

echo "Syncing version ${VERSION} (${VERSION_V}) from Chart.yaml..."

# Helm chart appVersion
sed -i "s/^appVersion:.*/appVersion: \"${VERSION}\"/" "${ROOT}/helm/gateforge/Chart.yaml"

# Maven artifact version (project root <version>, not dependency versions)
sed -i "0,/<version>/{s/<version>[^<]*<\/version>/<version>${VERSION}<\/version>/}" "${ROOT}/backend/pom.xml"

for pkg in gateforge-devhub-plugin gateforge-devhub-frontend; do
  sed -i "s/\"version\": \"[^\"]*\"/\"version\": \"${VERSION}\"/" "${ROOT}/${pkg}/package.json"
done

# Helm default image tags
sed -i "s/tag: v[0-9.]*$/tag: ${VERSION_V}/" "${ROOT}/helm/gateforge/values.yaml"

# DevHub plugin health endpoint
sed -i "s/version: '[0-9.]*'/version: '${VERSION}'/" "${ROOT}/gateforge-devhub-plugin/src/router.ts"

# User-facing docs (version strings only — not historical chart entries in index.yaml)
sed -i "s/>v[0-9.]*</>${VERSION_V}</g" "${ROOT}/docs/index.html"
sed -i "s/NEW v[0-9.]*/NEW ${VERSION_V}/" "${ROOT}/docs/index.html"
sed -i "s/GateForge v[0-9.]*/GateForge ${VERSION_V}/" "${ROOT}/docs/index.html"
sed -i "s/>0\.[0-9.]* (<code>quay.io\/maximilianopizarro\/gateforge-devhub-plugin<\/code>)/>${VERSION} (<code>quay.io\/maximilianopizarro\/gateforge-devhub-plugin<\/code>)/" "${ROOT}/docs/index.html"
sed -i "s/<td>v[0-9.]*<\/td><td>Backend image tag<\/td>/<td>${VERSION_V}<\/td><td>Backend image tag<\/td>/" "${ROOT}/docs/index.html"
sed -i "s/<td>v[0-9.]*<\/td><td>Frontend image tag<\/td>/<td>${VERSION_V}<\/td><td>Frontend image tag<\/td>/" "${ROOT}/docs/index.html"
sed -i "s/gateforge-devhub-plugin:[0-9.]*!/gateforge-devhub-plugin:${VERSION}!/g" "${ROOT}/docs/index.html"
sed -i "s/gateforge-devhub-frontend-plugin:[0-9.]*!/gateforge-devhub-frontend-plugin:${VERSION}!/g" "${ROOT}/docs/index.html"
sed -i "s/targetRevision: \"[0-9.]*\"/targetRevision: \"${VERSION}\"/" "${ROOT}/docs/index.html"

sed -i "s/> \\*\\*v[0-9.]*\\*\\* --> **${VERSION_V}** --/" "${ROOT}/README.md"
sed -i "s/Key Features (v[0-9.]*)/Key Features (${VERSION_V})/" "${ROOT}/README.md"
sed -i "s/Phase 6: APICast Discovery and Migration (v[0-9.]*)/Phase 6: APICast Discovery and Migration (${VERSION_V})/" "${ROOT}/README.md"
sed -i "s/| \`backend.image.tag\` | v[0-9.]* |/| \`backend.image.tag\` | ${VERSION_V} |/" "${ROOT}/README.md"
sed -i "s/| \`frontend.image.tag\` | v[0-9.]* |/| \`frontend.image.tag\` | ${VERSION_V} |/" "${ROOT}/README.md"

sed -i "s/| \`backend.image.tag\` | \`v[0-9.]*\` |/| \`backend.image.tag\` | \`${VERSION_V}\` |/" "${ROOT}/helm/gateforge/README.md"
sed -i "s/| \`frontend.image.tag\` | \`v[0-9.]*\` |/| \`frontend.image.tag\` | \`${VERSION_V}\` |/" "${ROOT}/helm/gateforge/README.md"

echo "Done. Review git diff before committing."
