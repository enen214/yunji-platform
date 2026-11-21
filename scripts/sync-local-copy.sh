#!/usr/bin/env bash
#
# sync-local-copy.sh —— 仓库 → 桌面副本 单向下发脚本
#
# 方向：只写桌面（/Users/rcj/Desktop/hd），绝不写仓库。
#       桌面副本只是运行产物；仓库是唯一真源。
#       桌面侧如有改动，必须人工判断后回灌仓库，禁止用桌面覆盖仓库。
#
# 用法：
#   bash scripts/sync-local-copy.sh            # 实际同步 + 自检
#   bash scripts/sync-local-copy.sh --dry-run  # 只打印将要传输/删除的清单
#
# 白名单（桌面侧保留、不被覆盖）：
#   target/、.idea/、*.log、export-report.json、.DS_Store、
#   config/、.run/、agri-admin/src/main/resources/application-local.yml
#
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_BACKEND="$REPO_ROOT/backend"
SRC_CONTRACTS="$REPO_ROOT/contracts"
DST="/Users/rcj/Desktop/hd"

DRY_RUN=0
if [ "${1:-}" = "--dry-run" ]; then
  DRY_RUN=1
elif [ -n "${1:-}" ]; then
  echo "未知参数：$1（仅支持 --dry-run）" >&2
  exit 2
fi

if [ ! -d "$SRC_BACKEND" ] || [ ! -d "$DST" ]; then
  echo "路径不存在：$SRC_BACKEND 或 $DST" >&2
  exit 2
fi

# 桌面侧白名单：这些路径不参与覆盖，也不参与删除
RSYNC_EXCLUDES=(
  --exclude 'target'
  --exclude '.idea'
  --exclude '*.log'
  --exclude 'export-report.json'
  --exclude '.DS_Store'
  --exclude '/config'
  --exclude '/.run'
  --exclude '/agri-admin/src/main/resources/application-local.yml'
  --exclude '/contracts'   # contracts 由第二次同步单独下发
)

RSYNC_OPTS=(-a --delete -i)
if [ "$DRY_RUN" -eq 1 ]; then
  RSYNC_OPTS+=(--dry-run)
fi

rc=0

echo "==> [1/2] backend/  ->  hd/"
rsync "${RSYNC_OPTS[@]}" "${RSYNC_EXCLUDES[@]}" "$SRC_BACKEND/" "$DST/" || rc=$?

echo "==> [2/2] contracts/  ->  hd/contracts/"
rsync "${RSYNC_OPTS[@]}" "$SRC_CONTRACTS/" "$DST/contracts/" || rc=$?

if [ "$rc" -ne 0 ]; then
  echo "rsync 失败，退出码 $rc" >&2
  exit "$rc"
fi

if [ "$DRY_RUN" -eq 1 ]; then
  echo "==> dry-run 完成，未写入任何文件，跳过自检。"
  exit 0
fi

echo "==> 自检"

# 规则 1：忽略白名单与换行符后，两侧 diff 必须为空。
# 说明：diff 的 -x 只能按 basename 匹配，无法锚定根目录，因此根级白名单目录
#       （config/、.run/）与 contracts 在输出中按路径前缀过滤剔除。
fail=0

backend_diff="$(
  diff -rq --strip-trailing-cr \
    -x target -x .idea -x '*.log' -x export-report.json -x .DS_Store \
    "$SRC_BACKEND" "$DST" 2>&1 \
  | grep -vE "(/backend|/hd)/(config|\.run)/|: (config|\.run|contracts)\$|/agri-admin/src/main/resources/application-local\.yml" \
  || true
)"
if [ -n "$backend_diff" ]; then
  echo "[FAIL] 自检 1：backend 与桌面副本仍有差异：" >&2
  echo "$backend_diff" >&2
  fail=1
fi

contracts_diff="$(diff -rq --strip-trailing-cr "$SRC_CONTRACTS" "$DST/contracts" 2>&1 || true)"
if [ -n "$contracts_diff" ]; then
  echo "[FAIL] 自检 1：contracts 与桌面副本仍有差异：" >&2
  echo "$contracts_diff" >&2
  fail=1
fi

# 规则 2：桌面 application-local.yml 的 secret 必须非空，防止登录被覆盖成空串。
local_yml="$DST/agri-admin/src/main/resources/application-local.yml"
if [ ! -f "$local_yml" ] \
  || ! grep -Eq '^[[:space:]]*secret:[[:space:]]*[^[:space:]]+' "$local_yml"; then
  echo "[FAIL] 自检 2：$local_yml 的 secret 为空或文件不存在" >&2
  fail=1
fi

# 规则 3：桌面侧 CRLF 文件数必须为 0。
# 白名单里的编译产物、编辑器状态与日志（target/、.idea/、*.log、export-report.json）
# 不是源码，不参与统计。
crlf_files="$(
  grep -rlI --exclude-dir=.idea --exclude-dir=target --exclude-dir=.git \
    --exclude='*.log' --exclude=export-report.json -- $'\r' "$DST" 2>/dev/null
)"
crlf_count="$(printf '%s' "$crlf_files" | grep -c . || true)"
if [ "$crlf_count" != "0" ]; then
  echo "[FAIL] 自检 3：桌面侧仍有 $crlf_count 个 CRLF 文件" >&2
  printf '%s\n' "$crlf_files" | head -20 >&2
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  echo "自检未通过。" >&2
  exit 1
fi

echo "[OK] 自检 1/3：diff 无输出（忽略白名单与换行符）"
echo "[OK] 自检 2/3：桌面 application-local.yml 的 secret 非空"
echo "[OK] 自检 3/3：桌面侧 CRLF 文件数为 0"
