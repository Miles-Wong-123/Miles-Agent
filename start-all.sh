#!/usr/bin/env bash

# 一键启动 Miles-Agent：先编一遍前端，再启动后端（单进程模式）。
# 浏览器访问 http://localhost:10010/api/ 即可看到 Vue 前端。
#
# 开发模式（双进程、前端热重载）请改用：
#   终端 A: bash start-miles-agent.sh
#   终端 B: cd web && pnpm dev   # 然后访问 http://localhost:5173/api/

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 选择可用的 pnpm 命令：优先全局，否则退回到 npx pnpm@10.34.3。
if command -v pnpm >/dev/null 2>&1; then
  PNPM="pnpm"
else
  PNPM="npx --yes pnpm@10.34.3"
fi

echo "==> Building web/ frontend..."
cd "$ROOT_DIR/web"

if [[ ! -d node_modules ]]; then
  echo "(installing web/ dependencies — first run only)"
  $PNPM install
fi

$PNPM build

echo "==> Frontend build done. Starting backend..."
exec bash "$ROOT_DIR/start-miles-agent.sh"
