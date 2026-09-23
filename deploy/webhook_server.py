#!/usr/bin/env python3
"""Small GitHub push webhook receiver for a dedicated deployment host."""

import hashlib
import hmac
import json
import logging
import os
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


SECRET = os.environ["WEBHOOK_SECRET"].encode("utf-8")
REPOSITORY = os.environ.get("GITHUB_REPOSITORY", "itwebs-corporate/itwebs-website-backend-04-2026")
DEPLOY_REF = os.environ.get("DEPLOY_REF", "refs/heads/dev")
DEPLOY_SCRIPT = os.environ.get("DEPLOY_SCRIPT", "/srv/itwebs/backend/deploy/deploy.sh")
BIND = os.environ.get("WEBHOOK_BIND", "127.0.0.1")
PORT = int(os.environ.get("WEBHOOK_PORT", "8765"))
MAX_BODY = 1024 * 1024


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/github-webhook":
            self.send_error(404)
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_error(400)
            return
        if length <= 0 or length > MAX_BODY:
            self.send_error(413)
            return

        body = self.rfile.read(length)
        expected = "sha256=" + hmac.new(SECRET, body, hashlib.sha256).hexdigest()
        signature = self.headers.get("X-Hub-Signature-256", "")
        if not hmac.compare_digest(signature, expected):
            self.send_error(401)
            return

        event = self.headers.get("X-GitHub-Event", "")
        if event == "ping":
            self.respond(200, b"pong")
            return
        if event != "push":
            self.respond(204)
            return

        try:
            payload = json.loads(body)
        except json.JSONDecodeError:
            self.send_error(400)
            return
        if payload.get("repository", {}).get("full_name") != REPOSITORY:
            self.send_error(403)
            return
        if payload.get("ref") != DEPLOY_REF or payload.get("deleted"):
            self.respond(204)
            return

        delivery = self.headers.get("X-GitHub-Delivery", "unknown")
        try:
            subprocess.Popen([DEPLOY_SCRIPT], close_fds=True, start_new_session=True)
        except OSError:
            logging.exception("Could not start deployment for delivery %s", delivery)
            self.send_error(500)
            return
        logging.info("Deployment queued for delivery %s, ref %s", delivery, DEPLOY_REF)
        self.respond(202, b"deployment queued")

    def do_GET(self):
        if self.path == "/health":
            self.respond(200, b"ok")
        else:
            self.send_error(404)

    def respond(self, status, body=b""):
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    if len(SECRET) < 32:
        raise SystemExit("WEBHOOK_SECRET must contain at least 32 characters")
    server = ThreadingHTTPServer((BIND, PORT), Handler)
    logging.info("Listening on %s:%s for %s %s", BIND, PORT, REPOSITORY, DEPLOY_REF)
    server.serve_forever()
