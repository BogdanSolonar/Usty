#!/usr/bin/env bash
# Відновлення на сервері з папки, де лежать db.dump і uploads.tar.gz
# (бекап із scripts/backup.sh або експорт локальної бази з scripts/export-local.ps1):
#   bash scripts/restore.sh usty-export
# УВАГА: поточна база на сервері ЗАМІНЮЄТЬСЯ вмістом бекапу.
set -euo pipefail
cd "$(dirname "$0")/.."
C="${COMPOSE:-docker compose -f docker-compose.prod.yml}"
UPLOADS="${UPLOADS:-data/uploads}"   # та сама папка, що змонтована в бекенд (docker-compose.prod.yml)
DIR="${1:?Вкажіть папку з db.dump і uploads.tar.gz}"
[ -f "$DIR/db.dump" ] || { echo "Немає $DIR/db.dump"; exit 1; }

read -r -p "База на сервері буде замінена даними з $DIR. Продовжити? (yes/no) " answer
[ "$answer" = "yes" ] || { echo "Скасовано"; exit 1; }

$C up -d db
until $C exec -T db pg_isready -U planning -d planning >/dev/null 2>&1; do sleep 1; done
$C stop backend >/dev/null 2>&1 || true

$C exec -T db psql -q -U planning -d postgres -c "drop database if exists planning" -c "create database planning"
$C exec -T db pg_restore -U planning -d planning --no-owner --no-acl < "$DIR/db.dump"

if [ -f "$DIR/uploads.tar.gz" ]; then
  mkdir -p "$UPLOADS"
  tar -xzf "$DIR/uploads.tar.gz" -C "$UPLOADS"
fi

$C up -d
echo "Готово: база й фото відновлені, бекенд перезапущено"
