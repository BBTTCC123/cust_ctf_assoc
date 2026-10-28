#!/usr/bin/env bash
# 每日备份：数据库 dump + 上传目录 + 清单，打成一个加密包，再尝试复制到异地对象存储。
set -Eeuo pipefail

BACKUP_DIR=/var/backup
UPLOAD_DIR=/srv/xyx
STAMP=$(date +%Y%m%d-%H%M%S)
WORK="$BACKUP_DIR/work-$STAMP"
OUT="$BACKUP_DIR/assoc-$STAMP.tar.gz.enc"
CNF=/etc/xyx/my.cnf
PASS_FILE=/etc/xyx/backup.pass

# 启动前预检：缺文件就明确报错，不要等到 mysqldump 才失败
for f in "$CNF" "$PASS_FILE"; do
  if [ ! -r "$f" ]; then
    echo "缺少可读的 $f。" >&2
    echo "请按 docs/部署清单.md 创建最小权限的 my.cnf 与 backup.pass，并 chmod 600。" >&2
    exit 1
  fi
done
if ! mysql --defaults-extra-file="$CNF" -e 'SELECT 1' >/dev/null 2>&1; then
  echo "数据库连接预检失败：请检查 $CNF 中的账号、密码和 127.0.0.1:3306 连通性。" >&2
  exit 1
fi

mkdir -p "$WORK"
mysqldump --single-transaction --quick --defaults-extra-file="$CNF" \
  assoc_dev > "$WORK/assoc_dev.sql"

if [ -d "$UPLOAD_DIR" ]; then
  tar -C "$UPLOAD_DIR" -cf "$WORK/uploads.tar" .
fi

(
  cd "$WORK"
  sha256sum assoc_dev.sql > MANIFEST.txt
  if [ -f uploads.tar ]; then
    sha256sum uploads.tar >> MANIFEST.txt
  fi
)

tar -C "$WORK" -czf "$BACKUP_DIR/assoc-$STAMP.tar.gz" .
openssl enc -aes-256-cbc -pbkdf2 -pass file:"$PASS_FILE" \
  -in "$BACKUP_DIR/assoc-$STAMP.tar.gz" -out "$OUT"
rm -f "$BACKUP_DIR/assoc-$STAMP.tar.gz"
rm -rf "$WORK"

# 本地只留 7 天；异地副本与版本控制的保留策略由对象存储生命周期负责
find "$BACKUP_DIR" -name 'assoc-*.tar.gz.enc' -mtime +7 -delete
echo "本地备份完成：$OUT"

# 异地复制：必须在 crontab 或 /etc/xyx/backup.env 里设置 OFFSITE_REMOTE（rclone 目标，如 oss:assoc-backup/）
if [ -n "${OFFSITE_REMOTE:-}" ]; then
  if command -v rclone >/dev/null 2>&1; then
    rclone copy "$OUT" "$OFFSITE_REMOTE" --retries 3 --log-level INFO
    echo "已复制到异地：$OFFSITE_REMOTE"
  else
    echo "!! 已设置 OFFSITE_REMOTE，但没有安装 rclone，异地复制未执行" >&2
    exit 2
  fi
else
  echo "!! 未设置 OFFSITE_REMOTE，本次备份只在本机，不满足异地备份要求" >&2
  exit 2
fi
