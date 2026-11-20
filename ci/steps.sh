# shellcheck shell=bash
# CI configuration of sk-demo (deelproduct Bestellen with the stubs), read by `fcd ci` (fcd-demo-pipeline).
# The steps, gates and log lines are in fcd; here only what is specific to this repository.

CI_COMPOSE_FILES="docker-compose.yml docker-compose.stubs.yml"
CI_SUBPRODUCT_CONTAINERS="bestellen-frontend bestellen-backend"
CI_OPENAPI="contracts/order.openapi.yaml bestellen/contracts/bestellen-api.openapi.yaml"
# Contract checks in the integration step: name=spec@url (inside the network of the environment)
CI_CONTRACT_TARGETS="bestellen-backend=bestellen/contracts/bestellen-api.openapi.yaml@http://bestellen-backend:8080
stub-kitchen=contracts/order.openapi.yaml@http://stub-kitchen:8080
stub-bar=contracts/order.openapi.yaml@http://stub-bar:8080
stub-cash-register=contracts/order.openapi.yaml@http://stub-cash-register:8080"
# Checks that the code does not break the contract: no server errors, status codes, content types and
# responses as in the contract. Not (yet): positive_data_acceptance (business rules such as "item on the
# menu" are not in the schema) and unsupported_method (404 instead of 405). Both on the backlog of fcd-demo.
CI_SCHEMATHESIS_ARGS="--checks not_a_server_error,status_code_conformance,content_type_conformance,response_schema_conformance,response_headers_conformance,negative_data_rejection"
CI_READY_URL="http://bestellen-frontend:8080/api/menu"
CI_E2E_DIR="e2e"

MAVEN_PROJECTS="bestellen/backend stubs/kitchen stubs/bar stubs/cash-register"

mvn_r() { # project goals...
  local p=$1; shift
  docker run --rm -v "$R_REPO":/w -v fcd-m2:/root/.m2 -w "/w/$p" "$TOOL_MAVEN" mvn -B -q "$@"
}

ci_lint() { # ktlint (MIT) for Kotlin, Prettier (MIT) for the frontend
  local p rc=0
  for p in $MAVEN_PROJECTS; do
    echo "== ktlint $p"
    mvn_r "$p" com.github.gantsign.maven:ktlint-maven-plugin:3.7.1:check || rc=1
  done
  echo "== prettier bestellen/frontend"
  docker run --rm -e npm_config_cache=/tmp/npm -v "$R_REPO/bestellen/frontend":/w:ro -w /w "$TOOL_NODE" \
    npx -y prettier@3.8.1 --check 'src/**/*.{ts,html,css}' || rc=1
  return $rc
}

ci_unit() { # the unit tests run in the image builds; the reports are exported
  local v
  v=$(ver_get "$R_REPO" BESTELLEN_FRONTEND_VERSION); unit_from_dockerfile bestellen-frontend bestellen/frontend "" --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" BESTELLEN_BACKEND_VERSION);  unit_from_dockerfile bestellen-backend bestellen bestellen/backend/Dockerfile --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" STUB_KITCHEN_VERSION);       unit_from_dockerfile stub-kitchen stubs/kitchen "" --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" STUB_BAR_VERSION);           unit_from_dockerfile stub-bar stubs/bar "" --build-arg "APP_VERSION=$v"
  v=$(ver_get "$R_REPO" STUB_CASH_REGISTER_VERSION); unit_from_dockerfile stub-cash-register stubs/cash-register "" --build-arg "APP_VERSION=$v"
}

ci_freshness() { # 0 = fresh enough, 1 = a library is a major version behind, 2 = could not be determined
  local p out major=0 minor=0 failed=0
  for p in $MAVEN_PROJECTS; do
    echo "== maven $p"
    out=$(mvn_r "$p" org.codehaus.mojo:versions-maven-plugin:2.25.2:display-dependency-updates -DprocessDependencyManagement=false -q -DoutputFile=/dev/stdout -DoutputEncoding=UTF-8 2>&1) || { failed=1; echo "$out" | tail -3; continue; }
    printf '%s\n' "$out" | grep -- '->' || echo "up to date"
    major=$((major + $(printf '%s\n' "$out" | awk '/->/ { split($(NF-2), a, "."); split($NF, b, "."); if (b[1] + 0 > a[1] + 0) n++ } END { print n + 0 }')))
    minor=$((minor + $(printf '%s\n' "$out" | grep -c -- '->' || true)))
  done
  echo "== npm bestellen/frontend"
  docker build -q --target build -t "fcd-tmp/sk-demo-frontend-build" "$R_REPO/bestellen/frontend" >/dev/null || failed=1
  out=$(docker run --rm fcd-tmp/sk-demo-frontend-build npm outdated --parseable 2>/dev/null || true)
  # --parseable: dir:name@wanted:name@current:name@latest:dependent (names can be scoped: @scope/name)
  local npm_awk='function ver(f) { return substr(f, match(f, /@[^@]*$/) + 1) } function name(f) { return substr(f, 1, match(f, /@[^@]*$/) - 1) }'
  printf '%s\n' "$out" | awk -F: "$npm_awk"' NF { print name($3) " " ver($3) " -> " ver($4) }'
  major=$((major + $(printf '%s\n' "$out" | awk -F: "$npm_awk"' NF { split(ver($3), a, "."); split(ver($4), b, "."); if (b[1] + 0 > a[1] + 0) n++ } END { print n + 0 }')))
  minor=$((minor + $(printf '%s\n' "$out" | grep -c . || true)))
  echo "behind: $major major, $minor in total"
  [ "$major" -gt 0 ] && return 1
  [ "$failed" -eq 1 ] && return 2
  [ "$minor" -gt 0 ] && s_warn "versheid: $minor libraries hebben een nieuwere minor- of patchversie"
  return 0
}
