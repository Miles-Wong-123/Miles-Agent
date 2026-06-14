#!/usr/bin/env bash

set -euo pipefail

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

stop_container_if_running() {
  local container_name="$1"
  local service_name="$2"

  if ! container_exists "$container_name"; then
    echo "Container not found: $container_name"
    return 0
  fi

  if container_running "$container_name"; then
    echo "Stopping $service_name..."
    docker stop "$container_name" >/dev/null
    echo "$service_name stopped"
  else
    echo "$service_name is already stopped"
  fi
}

stop_app_by_port() {
  local pids
  pids="$(lsof -tiTCP:"$APP_PORT" -sTCP:LISTEN || true)"

  if [[ -z "$pids" ]]; then
    echo "Miles-Agent is not running on port $APP_PORT"
    return 0
  fi

  echo "Stopping Miles-Agent on port $APP_PORT..."
  kill $pids

  for _ in {1..15}; do
    if ! lsof -tiTCP:"$APP_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
      echo "Miles-Agent stopped"
      return 0
    fi
    sleep 1
  done

  echo "Miles-Agent did not stop gracefully, forcing shutdown..."
  kill -9 $pids
  echo "Miles-Agent force stopped"
}

require_command docker
require_command lsof

stop_app_by_port
stop_container_if_running "$REDIS_CONTAINER" "redis"
stop_container_if_running "$PG_CONTAINER" "pgvector"
