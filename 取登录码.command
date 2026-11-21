#!/bin/bash
# 云迹本地验收登录码 —— 双击运行，码自动复制到剪贴板（2 分钟有效）
# 后端需已在运行（IDEA 点 DromaraApplication-local 启动，或在终端跑 jar）

SECRET="yunji-local-secret-2026"
API="http://127.0.0.1:18088"
USER_ID="91000000000000001"   # alice = 本地学生甲

echo "正在向后端申请登录码..."
RESP=$(curl -s --noproxy '*' -X POST "$API/api/local/login-codes" \
  -H "Content-Type: application/json" \
  -H "X-Local-Secret: $SECRET" \
  -d "{\"userId\":$USER_ID,\"appId\":\"local-client\"}")

if [ -z "$RESP" ]; then
  echo "❌ 连不上后端 $API"
  echo "   先在 IDEA 里启动 DromaraApplication-local，或在终端跑后端 jar"
  echo ""
  read -n 1 -p "按任意键关闭..."
  exit 1
fi

CODE=$(echo "$RESP" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['code'])" 2>/dev/null)

if [ -z "$CODE" ]; then
  echo "❌ 申请失败：$RESP"
  echo ""
  read -n 1 -p "按任意键关闭..."
  exit 1
fi

printf "%s" "$CODE" | pbcopy
echo ""
echo "✅ 登录码已复制到剪贴板（2 分钟内有效）："
echo ""
echo "   $CODE"
echo ""
echo "   去小程序登录页 → 「本地验收登录」→ 输入框粘贴 → 点「登录合成用户」"
echo ""
read -n 1 -p "按任意键关闭..."
