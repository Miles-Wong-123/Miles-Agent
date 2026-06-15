#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_DIR="$ROOT_DIR/web"
SERVER_DIR="$ROOT_DIR/server"
PG_CONTAINER="miles-agent-pgvector"
REDIS_CONTAINER="miles-agent-redis"
APP_PORT=10010

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name"
    exit 1
  fi
}

container_exists() {
  local container_name="$1"
  docker ps -a --format '{{.Names}}' | grep -Fxq "$container_name"
}

container_running() {
  local container_name="$1"
  [[ "$(docker inspect -f '{{.State.Running}}' "$container_name")" == "true" ]]
}

wait_for_port() {
  local port="$1"
  local service_name="$2"

  for _ in {1..20}; do
    if bash -c "exec 3<>/dev/tcp/127.0.0.1/$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "$service_name did not become ready on port $port"
  exit 1
}

ensure_container_running() {
  local container_name="$1"
  local port="$2"
  local service_name="$3"

  if ! container_exists "$container_name"; then
    echo "Container not found: $container_name"
    exit 1
  fi

  if container_running "$container_name"; then
    echo "$service_name is already running"
  else
    echo "Starting $service_name..."
    docker start "$container_name" >/dev/null
  fi

  wait_for_port "$port" "$service_name"
  echo "$service_name is ready on port $port"
}

build_frontend() {
  echo "Building web frontend..."

  cd "$WEB_DIR"

  if command -v pnpm >/dev/null 2>&1; then
    pnpm build
    return 0
  fi

  if [[ -x "./node_modules/.bin/vue-tsc" && -x "./node_modules/.bin/vite" ]]; then
    ./node_modules/.bin/vue-tsc --noEmit
    ./node_modules/.bin/vite build
    return 0
  fi

  echo "Frontend dependencies are missing. Please run 'cd web && pnpm install' first."
  exit 1
}

require_command docker
require_command bash
require_command lsof

cd "$ROOT_DIR"

ensure_container_running "$PG_CONTAINER" 5432 "pgvector"
ensure_container_running "$REDIS_CONTAINER" 6380 "redis"

if lsof -nP -iTCP:"$APP_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Miles-Agent is already running on port $APP_PORT"
  exit 0
fi

build_frontend

echo "Starting Miles-Agent..."
cd "$SERVER_DIR"
exec ./mvnw spring-boot:run
