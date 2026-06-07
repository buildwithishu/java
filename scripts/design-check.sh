#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESIGN_DIR="$ROOT_DIR/docs/design"
HLD="$DESIGN_DIR/HLD.md"
LLD="$DESIGN_DIR/LLD.md"

usage() {
  cat <<'USAGE'
Usage:
  scripts/design-check.sh --init   Create docs/design/HLD.md and LLD.md from templates if missing
  scripts/design-check.sh          Validate required HLD/LLD sections and Java layering rules

Required before implementation:
  - HLD: context, goals, architecture, components, data flow, security, testing
  - LLD: scope, class design, method contracts, patterns, database details, coding checklist, tests

Coding-standard checks:
  - Views must not import DAO classes or contain SQL statements
  - DAOs must not import JavaFX classes
  - Main source must not contain obvious hardcoded secret assignments
USAGE
}

copy_if_missing() {
  local template="$1"
  local target="$2"

  if [[ -f "$target" ]]; then
    echo "exists: ${target#$ROOT_DIR/}"
    return
  fi

  cp "$template" "$target"
  echo "created: ${target#$ROOT_DIR/}"
}

require_file() {
  local file="$1"

  if [[ ! -f "$file" ]]; then
    echo "missing: ${file#$ROOT_DIR/}"
    echo "run: scripts/design-check.sh --init"
    exit 1
  fi
}

require_heading() {
  local file="$1"
  local heading="$2"

  if ! grep -qiE "^##[[:space:]]+$heading([[:space:]]*$|[[:space:]]+)" "$file"; then
    echo "missing heading in ${file#$ROOT_DIR/}: ## $heading"
    exit 1
  fi
}

reject_placeholder_title() {
  local file="$1"

  if grep -q "<.*>" "$file"; then
    echo "placeholder still present in ${file#$ROOT_DIR/}"
    exit 1
  fi
}

reject_pattern() {
  local path="$1"
  local pattern="$2"
  local message="$3"

  if [[ ! -d "$path" ]]; then
    return
  fi

  local matches
  matches="$(grep -RInE "$pattern" "$path" || true)"
  if [[ -n "$matches" ]]; then
    echo "$message"
    echo "$matches"
    exit 1
  fi
}

validate_java_layering() {
  local src="$ROOT_DIR/src/main/java"
  local view_dir="$src/com/eventportal/view"
  local dao_dir="$src/com/eventportal/dao"

  reject_pattern "$view_dir" "import[[:space:]]+com\\.eventportal\\.dao\\." "view layer must not import DAO classes"
  reject_pattern "$view_dir" "\\b(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER)\\b" "view layer must not contain SQL statements"
  reject_pattern "$dao_dir" "import[[:space:]]+javafx\\." "DAO layer must not import JavaFX classes"
  reject_pattern "$src" "(password|secret|token|api[_-]?key)[[:space:]]*=[[:space:]]*\"[^\"]+\"" "possible hardcoded secret assignment found"
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "${1:-}" == "--init" ]]; then
  mkdir -p "$DESIGN_DIR"
  copy_if_missing "$DESIGN_DIR/HLD_TEMPLATE.md" "$HLD"
  copy_if_missing "$DESIGN_DIR/LLD_TEMPLATE.md" "$LLD"
  exit 0
fi

if [[ $# -gt 0 ]]; then
  usage
  exit 1
fi

require_file "$HLD"
require_file "$LLD"

for heading in Context Goals Architecture Components "Data Flow" "Security And Validation" "Testing Strategy"; do
  require_heading "$HLD" "$heading"
done

for heading in Scope "Class Design" "Method Contracts" "Patterns Used" "Database Details" "Coding Standard Checklist" "Test Cases"; do
  require_heading "$LLD" "$heading"
done

reject_placeholder_title "$HLD"
reject_placeholder_title "$LLD"
validate_java_layering

echo "design and coding standards pass"
