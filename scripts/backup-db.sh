#!/usr/bin/env bash
# 每日备份：数据库 dump + 上传目录 + 清单，打成一个加密包。
set -Eeuo pipefail

BACKUP_DIR=/var/backup
UPLOAD_DIR=/srv/xyx
STAMP=$(date +%Y%m%d)
WORK="$BACKUP_DIR/work-$STAMP"
OUT="$BACKUP_DIR/assoc-$STAMP.tar.gz.enc"
PASS_FILE=/etc/xyx/backup.pass

mkdir -p "$WORK"
mysqldump --single-transaction --quick --defaults-extra-file=/etc/xyx/my.cnf \
  assoc_dev > "$WORK/assoc_dev.sql"

if [ -d "$UPLOAD_DIR" ]; then
  tar -C "$UPLOAD_DIR" -cf "$WORK/uploads.tar" .
fi

( cd "$WORK" && sha256sum assoc_dev.sql > MANIFEST.txt )

tar -C "$WORK" -czf "$BACKUP_DIR/assoc-$STAMP.tar.gz" .
openssl enc -aes-256-cbc -pbkdf2 -pass file:"$PASS_FILE" \
  -in "$BACKUP_DIR/assoc-$STAMP.tar.gz" -out "$OUT"
rm -f "$BACKUP_DIR/assoc-$STAMP.tar.gz"
rm -rf "$WORK"

# 本地只留 7 天
find "$BACKUP_DIR" -name 'assoc-*.tar.gz.enc' -mtime +7 -delete
echo "备份完成：$OUT"
