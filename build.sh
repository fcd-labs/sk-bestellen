#!/usr/bin/env bash
# The only way to build and deploy. GitHub Actions calls this same script.
#
#   ./build.sh build    build all images; unit tests run inside the image build
#   ./build.sh scan     scan repository and images for vulnerabilities, secrets and misconfiguration
#   ./build.sh deploy   deploy the whole sub-product plus the stubs (docker-compose)
#   ./build.sh ci       build, then scan (what GitHub Actions runs)
#   ./build.sh all      build, scan, then deploy (default)
#
# A HIGH or CRITICAL finding stops the script, so nothing insecure is deployed.
set -euo pipefail

cd "$(dirname "$0")"

APP_VERSION="$(tr -d '[:space:]' < VERSION)"
export APP_VERSION

TRIVY_IMAGE="aquasec/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb"
SCAN_DIR="$PWD/.scan"

compose() {
  docker compose -f docker-compose.yml -f docker-compose.stubs.yml "$@"
}

trivy() {
  docker run --rm \
    -v "$PWD":/repo:ro \
    -v "$SCAN_DIR":/scan \
    -v showkitchen-trivy-cache:/root/.cache \
    "$TRIVY_IMAGE" "$@"
}

build() {
  echo "==> Build ${APP_VERSION}"
  compose build
}

scan() {
  echo "==> Scan repository (dependencies, secrets, configuration)"
  mkdir -p "$SCAN_DIR"
  # --offline-scan: do not fetch poms from Maven Central (rate limits).
  # Java dependencies are fully checked in the image scan (they are in app.jar).
  trivy fs --quiet --exit-code 1 --severity HIGH,CRITICAL \
    --scanners vuln,secret,misconfig --offline-scan \
    --skip-dirs /repo/bestellen/frontend/node_modules \
    --skip-dirs /repo/.scan \
    /repo

  for image in $(compose config --images); do
    echo "==> Scan image ${image}"
    docker save -o "$SCAN_DIR/image.tar" "$image"
    trivy image --quiet --exit-code 1 --severity HIGH,CRITICAL --ignore-unfixed \
      --input /scan/image.tar
    rm -f "$SCAN_DIR/image.tar"
  done
}

deploy() {
  echo "==> Deploy ${APP_VERSION}"
  compose up -d --remove-orphans
  compose ps
}

case "${1:-all}" in
  build)  build ;;
  scan)   scan ;;
  deploy) deploy ;;
  ci)     build; scan ;;
  all)    build; scan; deploy ;;
  *)
    echo "Usage: $0 [build|scan|deploy|ci|all]" >&2
    exit 1
    ;;
esac
