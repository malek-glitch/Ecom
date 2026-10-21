#!/bin/sh
# Restores a dump made by the backup service, replacing the current data.
#
#   deploy/backup/restore.sh backups/liftoff-20260101T030000Z.dump
#
# Run from the repository root on the server. The app is stopped during the restore.
set -eu

dump=${1:?usage: deploy/backup/restore.sh <file.dump>}
[ -f "$dump" ] || { echo "No such file: $dump" >&2; exit 1; }
compose="docker compose -f compose.prod.yaml"

printf 'This replaces the database with %s. Type "restore" to continue: ' "$dump"
read -r answer
[ "$answer" = "restore" ] || { echo "Cancelled."; exit 1; }

$compose stop backend frontend
# shellcheck disable=SC2016 # expanded inside the container
$compose exec -T postgres sh -c 'pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "$dump"
$compose start backend frontend
echo "Restored $dump."
