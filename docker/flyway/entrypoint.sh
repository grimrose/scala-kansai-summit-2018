#!/usr/bin/env bash

set -eu

_count=1

#waiting for postgres
until PGUSER=${POSTGRES_USER} PGPASSWORD=${POSTGRES_PASSWORD} PGDATABASE=${POSTGRES_DB} pg_isready; do
  >&2 echo "Postgres is unavailable - sleeping"
  if [ ${_count} -gt 10 ]; then
    exit 1
  fi

  _count=$((_count+1))

  sleep 1
done

>&2 echo "Postgres is up - executing command"

flyway $@
