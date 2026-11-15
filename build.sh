#!/usr/bin/env bash
# The only way to build and deploy. GitHub Actions calls this same script.
#
#   ./build.sh build    build all images; unit tests run inside the image build
#   ./build.sh scan     scan repository and images for vulnerabilities, secrets and misconfiguration
#   ./build.sh deploy   deploy the whole sub-product plus the stubs (docker-compose)
#   ./build.sh e2e      run the end-to-end tests (Playwright) against the deployed environment
#   ./build.sh ci       build, scan, deploy, e2e (what GitHub Actions runs, on its own runner)
#   ./build.sh all      build, scan, deploy, e2e (default)
#
# A HIGH or CRITICAL finding stops the script, so nothing insecure is deployed.
# A failing test stops the script too.
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

wait_until_ready() {
  echo "==> Wait until Bestellen is ready"
  for _ in $(seq 1 60); do
    if curl -sf -o /dev/null http://127.0.0.1:8080/api/menu; then return 0; fi
    sleep 1
  done
  echo "Bestellen did not become ready within 60 seconds" >&2
  compose ps >&2
  return 1
}

e2e() {
  wait_until_ready
  echo "==> End-to-end tests ${APP_VERSION}"
  docker build -t "showkitchen/e2e:${APP_VERSION}" e2e
  docker run --rm \
    --network showkitchen_default \
    --read-only --tmpfs /tmp \
    --cap-drop ALL --security-opt no-new-privileges:true \
    --shm-size 1g \
    "showkitchen/e2e:${APP_VERSION}"
}

case "${1:-all}" in
  build)  build ;;
  scan)   scan ;;
  deploy) deploy ;;
  e2e)    e2e ;;
  ci)     build; scan; deploy; e2e ;;
  all)    build; scan; deploy; e2e ;;
  *)
    echo "Usage: $0 [build|scan|deploy|e2e|ci|all]" >&2
    exit 1
    ;;
esac
