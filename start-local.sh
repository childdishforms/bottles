#!/bin/bash
set -eu
cd "$(dirname "$0")"

export WATER_DB_URL="${WATER_DB_URL:-jdbc:postgresql://127.0.0.1:5432/water}"
export WATER_DB_USER="${WATER_DB_USER:-water}"
if [ -z "${WATER_DB_PASSWORD:-}" ]; then
    IFS= read -r -s -p "Пароль локальной роли PostgreSQL: " WATER_DB_PASSWORD
    printf '\n'
fi
export WATER_DB_PASSWORD
exec java -jar target/water-journal-1.0.0.jar --server.port=18080
