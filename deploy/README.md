# Deploy via GitHub webhook

This setup assumes an Ubuntu/Debian server with Docker Compose, Python 3, Nginx, an HTTPS domain, and a `deploy` user that can run Docker. The receiver listens only on `127.0.0.1:8765`; Nginx exposes `/github-webhook` over HTTPS. Push events for one repository and branch trigger deployment after HMAC-SHA256 verification.

## 1. Prepare the server checkout

Give the server read access to the private repository with a read-only GitHub deploy key. Clone the repository as `deploy` to `/srv/itwebs/backend`, and create `/srv/itwebs/backend/.env` from `.env.example` with production values. Keep `.env` out of Git and restrict it to the deploy user (`chmod 600`). Ensure `deploy` can run `docker compose` without a password prompt.

The checkout must use a Git remote named `origin`. The deployment script fetches `DEPLOY_BRANCH`, switches to the fetched commit, builds the application image, starts Compose, and waits for `/v3/api-docs` on port 9999. The Dockerfile skips tests during its Maven build.

## 2. Configure the receiver

Generate a random secret on the server with `openssl rand -hex 32`. Put the *same* value into GitHub's webhook Secret field and `/etc/itwebs-webhook.env`:

```text
WEBHOOK_SECRET=<64-hex-character-secret>
GITHUB_REPOSITORY=itwebs-corporate/itwebs-website-backend-04-2026
DEPLOY_REF=refs/heads/dev
DEPLOY_BRANCH=dev
DEPLOY_DIR=/srv/itwebs/backend
DEPLOY_SCRIPT=/srv/itwebs/backend/deploy/deploy.sh
WEBHOOK_BIND=127.0.0.1
WEBHOOK_PORT=8765
```

Set the environment file owner to `root` and permissions to `600`; systemd reads it and passes the values to the service. Copy `deploy/itwebs-webhook.service` to `/etc/systemd/system/`, run `systemctl daemon-reload`, then `systemctl enable --now itwebs-webhook.service`.

The service file is an example for a server with a user and group both named `deploy`. Adjust those fields and paths to match the server. The webhook receiver uses Python's standard library, so it needs no additional Python packages.

## 3. Expose HTTPS endpoint

Place `deploy/nginx-location.conf.example` inside the existing HTTPS server block and reload Nginx after `nginx -t`. The public URL will be `https://<your-domain>/github-webhook`. Keep port 8765 closed externally; only Nginx needs to reach it.

## 4. Add the GitHub webhook

In the repository's **Settings → Webhooks → Add webhook**:

- **Payload URL:** `https://<your-domain>/github-webhook`
- **Content type:** `application/json`
- **Secret:** the same random secret as `WEBHOOK_SECRET`
- **Events:** Just the push event
- **Active:** enabled

GitHub sends a `ping` after creation. A valid ping returns `200`; a push to the configured branch returns `202` and queues a deployment. Pushes to other branches return `204`. A wrong signature returns `401`.

## Logs and rollout

Read receiver/deployment logs with `journalctl -u itwebs-webhook.service -f`; check containers with `docker compose --env-file .env ps` in the deployment directory. If the Java application fails to start, the deployment script logs Compose status and the last 80 app log lines. It exits nonzero and does not report a healthy deployment.

The webhook receiver accepts a signed request promptly; it does not wait for the Docker build. A `202` delivery response means deployment was queued, so confirm completion in server logs. `flock` serializes overlapping deployments, and each run fetches the latest commit from the configured branch.
