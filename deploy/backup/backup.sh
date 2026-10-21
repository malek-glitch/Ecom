#!/bin/sh
# Runs inside the `backup` service (compose.prod.yaml): dumps the database now, then every
# BACKUP_INTERVAL_SECONDS, and deletes dumps older than BACKUP_RETENTION_DAYS.
# Restore with deploy/backup/restore.sh.
set -eu
umask 077 # dumps hold password hashes and emails: readable by root only

: "${BACKUP_RETENTION_DAYS:=14}"
: "${BACKUP_INTERVAL_SECONDS:=86400}"

while true; do
	stamp=$(date -u +%Y%m%dT%H%M%SZ)
	file="/backups/${PGDATABASE}-${stamp}.dump"
	# Custom format: compressed, and restorable table by table.
	if pg_dump --format=custom --no-owner --file="${file}.partial"; then
		mv "${file}.partial" "${file}"
		echo "backup: wrote ${file} ($(du -h "${file}" | cut -f1))"
	else
		rm -f "${file}.partial"
		echo "backup: pg_dump failed" >&2
	fi
	find /backups -name "${PGDATABASE}-*.dump" -type f -mtime "+${BACKUP_RETENTION_DAYS}" -print -delete
	sleep "${BACKUP_INTERVAL_SECONDS}"
done
