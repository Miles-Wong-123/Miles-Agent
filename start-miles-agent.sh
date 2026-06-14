#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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
  docker inspect "$container_name" >/dev/null 2>&1
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

require_command docker
require_command bash

cd "$ROOT_DIR"

ensure_container_running "$PG_CONTAINER" 5432 "pgvector"
ensure_container_running "$REDIS_CONTAINER" 6380 "redis"

if lsof -nP -iTCP:"$APP_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Miles-Agent is already running on port $APP_PORT"
  exit 0
fi

echo "Starting Miles-Agent..."
cd "$ROOT_DIR/server"
exec ./mvnw spring-boot:run
