#!/usr/bin/env bash
# 发布脚本。用法： ./deploy.sh /tmp/cust-ctf-assoc-<版本>.jar
# 规矩：生产机上不跑编译，只接收本地构建好、带校验值的 jar。
set -Eeuo pipefail

APP_DIR=/opt/xyx
JAR="$1"
STAMP=$(date +%Y%m%d-%H%M%S)
REL="$APP_DIR/releases/$STAMP"

if [ ! -f "$JAR" ]; then
  echo "找不到 jar：$JAR" >&2
  exit 1
fi

mkdir -p "$REL"
cp "$JAR" "$REL/app.jar"

# 有 .sha256 就校验，没有就拒绝发布
if [ -f "$JAR.sha256" ]; then
  ( cd "$REL" && sha256sum -c "$JAR.sha256" --status ) || { echo "校验失败" >&2; exit 1; }
else
  echo "缺少 $JAR.sha256，拒绝发布" >&2
  exit 1
fi

PREV=$(readlink -f "$APP_DIR/current" || true)

rollback() {
  echo "!! 发布失败，切回 ${PREV:-无}"
  if [ -n "$PREV" ]; then
    ln -sfn "$PREV" "$APP_DIR/current"
    systemctl restart xyx || true
  fi
  exit 1
}
trap rollback ERR

# 迁移前先备份数据库
/usr/local/bin/backup-db.sh

ln -sfn "$REL" "$APP_DIR/current"
systemctl restart xyx

for _ in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "发布成功：$STAMP"
    trap - ERR
    exit 0
  fi
  sleep 2
done

rollback
