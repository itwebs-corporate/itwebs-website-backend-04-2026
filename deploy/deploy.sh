#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/srv/itwebs/backend}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-dev}"

if [[ ! -d "$DEPLOY_DIR/.git" ]]; then
  echo "Deployment checkout missing: $DEPLOY_DIR" >&2
  exit 1
fi
if [[ ! -f "$DEPLOY_DIR/.env" ]]; then
  echo "Deployment environment missing: $DEPLOY_DIR/.env" >&2
  exit 1
fi

exec 9>"$DEPLOY_DIR/.deploy.lock"
flock 9

echo "Deploying origin/$DEPLOY_BRANCH at $(date -u +%FT%TZ)"
git -C "$DEPLOY_DIR" fetch --depth=1 origin "$DEPLOY_BRANCH"
git -C "$DEPLOY_DIR" switch --detach FETCH_HEAD

cd "$DEPLOY_DIR"
docker compose --env-file .env up --build -d --remove-orphans

for attempt in {1..30}; do
  if curl --fail --silent --show-error --max-time 3 http://127.0.0.1:9999/v3/api-docs >/dev/null; then
    echo "Deployment healthy: $(git rev-parse --short HEAD)"
    exit 0
  fi
  sleep 2
done

echo "Application did not become healthy" >&2
docker compose --env-file .env ps >&2
docker compose --env-file .env logs --tail=80 app >&2
exit 1
