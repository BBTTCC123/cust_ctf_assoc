#!/usr/bin/env bash
# 发布脚本。用法： ./deploy.sh /tmp/cust-ctf-assoc-<版本>.jar
# 规矩：生产机上不跑编译，只接收本地构建好、带校验值的 jar。
set -Eeuo pipefail

APP_DIR=/opt/xyx
JAR="${1:-}"
STAMP=$(date +%Y%m%d-%H%M%S)
REL="$APP_DIR/releases/$STAMP"
CHECK="$JAR.sha256"

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "用法： ./deploy.sh /tmp/xxx.jar，且同目录要有 xxx.jar.sha256" >&2
  exit 1
fi
if [ ! -f "$CHECK" ]; then
  echo "缺少校验文件 $CHECK，拒绝发布" >&2
  exit 1
fi

mkdir -p "$REL"
cp "$JAR" "$REL/app.jar"

# 上传的 jar 会被改名为 app.jar，所以按原 .sha256 里记录的值与 app.jar 的实际哈希比对，
# 不能拿 sha256sum -c 去校验原文件名（那个文件已经不在了）。
EXPECTED=$(awk 'NR==1 { print tolower($1) }' "$CHECK")
ACTUAL=$(sha256sum "$REL/app.jar" | awk '{ print tolower($1) }')
if [ -z "$EXPECTED" ] || [ "$EXPECTED" != "$ACTUAL" ]; then
  echo "SHA256 校验失败：expected=$EXPECTED actual=$ACTUAL" >&2
  rm -rf "$REL"
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

prune_releases() {
  local current
  current=$(readlink -f "$APP_DIR/current")
  find "$APP_DIR/releases" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' \
    | sort -nr \
    | tail -n +6 \
    | cut -d' ' -f2- \
    | while IFS= read -r old; do
        if [ -n "$old" ] && [ "$old" != "$current" ]; then
          rm -rf -- "$old"
        fi
      done
}

# 迁移前先备份数据库；备份脚本自身会做 my.cnf / 口令文件预检
/usr/local/bin/backup-db.sh

ln -sfn "$REL" "$APP_DIR/current"
systemctl restart xyx

for _ in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "发布成功：$STAMP"
    prune_releases
    trap - ERR
    exit 0
  fi
  sleep 2
done

rollback
