#!/usr/bin/env bash
# Бекап на сервері: база (pg_dump) + усі фото -> backups/<дата>/
#   bash scripts/backup.sh
# Зберігає останні 14 бекапів. Щоденно через cron:  0 4 * * * cd ~/usty && bash scripts/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."
C="${COMPOSE:-docker compose -f docker-compose.prod.yml}"
UPLOADS="${UPLOADS:-data/uploads}"   # та сама папка, що змонтована в бекенд (docker-compose.prod.yml)

OUT="backups/$(date +%Y%m%d-%H%M)"
mkdir -p "$OUT"
$C exec -T db pg_dump -U planning -d planning -Fc > "$OUT/db.dump"
tar -czf "$OUT/uploads.tar.gz" -C "$UPLOADS" .
echo "Бекап готовий: $OUT ($(du -sh "$OUT" | cut -f1))"

ls -1dt backups/*/ | tail -n +15 | xargs -r rm -rf
