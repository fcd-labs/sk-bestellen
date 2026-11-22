# shellcheck shell=bash
# How this product is linted, tested and built, read by `fcd ci` (fcd-pipeline).
# Steps, order, gates, thresholds and explains are in the configuration of the showcase (fcd-demo/fcd); here only
# facts about this product.

CI_COMPOSE_FILES="docker-compose.yml docker-compose.stubs.yml"
CI_SUBPRODUCT_CONTAINERS="bestellen-frontend bestellen-backend"
# The source of each container: unchanged version and code means the container skips its steps (own lifecycle).
CI_CONTAINER_PATHS="bestellen-frontend=bestellen/frontend
bestellen-backend=bestellen/backend bestellen/contracts bestellen/menu"
# The contracts (and their compatibility rule) are in the configuration of the showcase (kind: Contract).
# Contract checks in the integration step: name=spec@url (inside the network of the environment)
CI_CONTRACT_TARGETS="bestellen-backend=bestellen/contracts/bestellen-api.openapi.yaml@http://bestellen-backend:8080
stub-kitchen=contracts/order.openapi.yaml@http://stub-kitchen:8080
stub-bar=contracts/order.openapi.yaml@http://stub-bar:8080
stub-cash-register=contracts/order.openapi.yaml@http://stub-cash-register:8080"
# Which contract checks run (and how many examples) is in the configuration of the showcase (Step integration-tests).
CI_READY_URL="http://bestellen-frontend:8080/api/menu"
# Test traffic is marked as test, so it never counts as an order of a guest in the key result.
CI_TEST_HEADERS="X-FCD-Test: true"
CI_E2E_DIR="e2e"

MAVEN_PROJECTS="bestellen/backend stubs/kitchen stubs/bar stubs/cash-register"

mvn_r() { # project goals...
  local p=$1; shift
  docker run --rm -v "$R_REPO":/w -v fcd-m2:/root/.m2 -w "/w/$p" "$TOOL_MAVEN" mvn -B -q "$@"
}

# Per container (the Pipeline passes it for steps of level container); without one: everything, stubs included.
# Stubs are not part of Bestellen: they have their own lifecycle and are not checked in the run of a container.
maven_projects_of() { # [container]
  case ${1:-} in
    bestellen-backend) echo bestellen/backend ;;
    bestellen-frontend) ;;
    *) echo "$MAVEN_PROJECTS" ;;
  esac
}

ci_lint() { # [container]: ktlint (MIT) for Kotlin, Prettier (MIT) for the frontend
  local p rc=0
  for p in $(maven_projects_of "${1:-}"); do
    echo "== ktlint $p"
    mvn_r "$p" com.github.gantsign.maven:ktlint-maven-plugin:3.7.1:check || rc=1
  done
  if [ -z "${1:-}" ] || [ "$1" = bestellen-frontend ]; then
    echo "== prettier bestellen/frontend"
    docker run --rm -e npm_config_cache=/tmp/npm -v "$R_REPO/bestellen/frontend":/w:ro -w /w "$TOOL_NODE" \
      npx -y prettier@3.8.1 --check 'src/**/*.{ts,html,css}' || rc=1
  fi
  return $rc
}

ci_unit() { # [container]: the unit tests run in the image builds; the reports are exported
  local v
  case ${1:-} in
    bestellen-frontend)
      v=$(ver_get "$R_REPO" BESTELLEN_FRONTEND_VERSION); unit_from_dockerfile bestellen-frontend bestellen/frontend "" --build-arg "APP_VERSION=$v"; return ;;
    bestellen-backend)
      v=$(ver_get "$R_REPO" BESTELLEN_BACKEND_VERSION); unit_from_dockerfile bestellen-backend bestellen bestellen/backend/Dockerfile --build-arg "APP_VERSION=$v"; return ;;
  esac
  v=$(ver_get "$R_REPO" BESTELLEN_FRONTEND_VERSION); unit_from_dockerfile bestellen-frontend bestellen/frontend "" --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" BESTELLEN_BACKEND_VERSION);  unit_from_dockerfile bestellen-backend bestellen bestellen/backend/Dockerfile --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" STUB_KITCHEN_VERSION);       unit_from_dockerfile stub-kitchen stubs/kitchen "" --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" STUB_BAR_VERSION);           unit_from_dockerfile stub-bar stubs/bar "" --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" STUB_CASH_REGISTER_VERSION); unit_from_dockerfile stub-cash-register stubs/cash-register "" --build-arg "APP_VERSION=$v"
}

ci_freshness() { # facts only: one line "freshness|part|name|current|latest" per library with a newer version.
  # The threshold (how far behind is allowed) is in the configuration of the showcase, not here.
  # Returns 2 when it could not be determined.
  local p out failed=0
  for p in $MAVEN_PROJECTS; do
    echo "== maven $p"
    out=$(docker run --rm -v "$R_REPO":/w -v fcd-m2:/root/.m2 -w "/w/$p" "$TOOL_MAVEN" mvn -B \
      org.codehaus.mojo:versions-maven-plugin:2.25.2:display-dependency-updates -DprocessDependencyManagement=false 2>&1) || { failed=1; continue; }
    # Lines like: [INFO]   group:artifact ......... 1.5.9 -> 3.0.9
    printf '%s\n' "$out" | grep -- ' -> ' | sed 's/^\[INFO\] *//' | awk 'NF { print "freshness|backend (maven)|" $1 "|" $(NF-2) "|" $NF }' | sort -u
  done
  echo "== npm frontend"
  docker build -q --target build -t fcd-tmp/sk-bestellen-frontend-build "$R_REPO/bestellen/frontend" >/dev/null || failed=1
  out=$(docker run --rm fcd-tmp/sk-bestellen-frontend-build npm outdated --parseable 2>/dev/null || true)
  # --parseable: dir:name@wanted:name@current:name@latest:dependent (names can be scoped: @scope/name)
  local npm_awk='function ver(f) { return substr(f, match(f, /@[^@]*$/) + 1) } function name(f) { return substr(f, 1, match(f, /@[^@]*$/) - 1) }'
  printf '%s\n' "$out" | awk -F: "$npm_awk"' NF { print "freshness|frontend (npm)|" name($3) "|" ver($3) "|" ver($4) }'
  [ "$failed" -eq 1 ] && return 2
  return 0
}
