# Deployment

This guide takes a fresh Linux server to a running app on your domain with HTTPS. It uses
`compose.prod.yaml` (plus `deploy/compose.storage.yaml` for uploads) and a
[Traefik](https://traefik.io) reverse proxy, which runs on the server and is shared by all
your apps:

```
                       ┌────────── Docker network `edge` ──────────┐
 Internet ─ 443 ─ Traefik ─┬─ /api/*   → liftoff-backend:8080       │
          ─  80 ─ (shared) └─ the rest → liftoff-frontend:3000      │
                       └───────────────────────────────────────────┘
                              app network: postgres · rustfs (opt) · backup (daily pg_dump)
```

Traefik is the only thing that publishes ports (80 and 443), and it gets and renews the TLS
certificates. The app's containers publish nothing: PostgreSQL, the API and RustFS are
reachable only from inside Docker.

Already running Traefik for other apps? You only add one routing file (step 4). Starting from an
empty server? `deploy/traefik/standalone/` is a ready-made Traefik (step 4 explains).

## 1. Server

Any Linux server with Docker works. For a small app:

- 2 vCPU and 4 GB of RAM if you build the images on the server, or 2 GB if CI builds them (step 9);
- 20 GB of disk;
- Ubuntu 24.04 LTS or Debian 13.

As root, create a user for deployments and install Docker:

```bash
adduser deploy && usermod -aG sudo deploy
rsync --archive --chown=deploy:deploy ~/.ssh /home/deploy   # reuse your SSH key
curl -fsSL https://get.docker.com | sh                      # Docker Engine + Compose plugin
usermod -aG docker deploy
```

Allow only SSH and the web ports through the firewall:

```bash
ufw allow OpenSSH && ufw allow 80/tcp && ufw allow 443/tcp && ufw allow 443/udp
ufw enable
```

Docker publishes ports past `ufw`. That's why only Traefik publishes any.

Recommended:

- turn on automatic security updates (`apt install unattended-upgrades`);
- turn off SSH password logins (`PasswordAuthentication no` in `/etc/ssh/sshd_config`);
- on a 2 GB server, add a swap file.

## 2. Domain

Create an `A` record (and `AAAA` for IPv6) pointing your domain, e.g. `app.example.com`, at the
server. Check it before going further, because Traefik needs it to get a certificate:

```bash
dig +short app.example.com     # must print the server's IP
```

## 3. Code and configuration

As `deploy`:

```bash
git clone https://github.com/<you>/<your-app>.git ~/app && cd ~/app
```

Create `.env` from scratch. Don't copy `.env.example`: its values are for development.

```bash
cat > .env <<EOF
COMPOSE_FILE=compose.prod.yaml
APP_NAME=Acme
APP_DOMAIN=app.example.com

POSTGRES_DB=acme
POSTGRES_USER=acme
POSTGRES_PASSWORD=$(openssl rand -base64 24 | tr -d '/+=')
JWT_SECRET=$(openssl rand -base64 48)

ADMIN_EMAIL=you@example.com
ADMIN_PASSWORD=$(openssl rand -base64 18)

MAIL_HOST=smtp.example-provider.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=Acme <no-reply@mail.example.com>
EOF
chmod 600 .env
cat .env   # note the generated ADMIN_PASSWORD
```

`COMPOSE_FILE` makes plain `docker compose` commands use the production file. That's
how every command below is written.

Optional additions:

- `GOOGLE_CLIENT_ID=...` for Google sign-in.
- Uploads with the bundled RustFS:
  - `COMPOSE_FILE=compose.prod.yaml:deploy/compose.storage.yaml` instead of the line above;
  - `STORAGE_ENABLED=true`;
  - generated `STORAGE_ACCESS_KEY` and `STORAGE_SECRET_KEY`.

  For a managed S3 service instead, keep `COMPOSE_FILE=compose.prod.yaml` and set the `STORAGE_*` variables for it.

[CONFIGURATION.md](CONFIGURATION.md) lists every variable.

## 4. Start

**a) Traefik and the routing file.** Traefik reaches the app over a Docker network called `edge`.

If you already run Traefik with a file provider and an `edge` network (as in `/srv/traefik`):

```bash
docker network inspect edge >/dev/null 2>&1 || docker network create edge
cp deploy/traefik/liftoff.yml /srv/traefik/dynamic/liftoff.yml
sed -i 's/app.example.com/YOUR.DOMAIN/g' /srv/traefik/dynamic/liftoff.yml
```

Traefik watches that directory and picks the file up at once. The file assumes entry points
named `web` and `websecure` and a certificate resolver named `letsencrypt`. Rename them in the
file if yours differ.

If you have no Traefik yet:

```bash
docker network create edge
cd deploy/traefik/standalone
mkdir -p dynamic letsencrypt
cp ../liftoff.yml dynamic/ && sed -i 's/app.example.com/YOUR.DOMAIN/g' dynamic/liftoff.yml
sed -i 's/you@example.com/YOUR@EMAIL/' traefik.yml        # Let's Encrypt contact
docker compose up -d && cd ../../..
```

**b) The app.**

```bash
docker compose up -d --build
docker compose logs -f backend           # Ctrl+C to stop following
```

The first build takes a few minutes. Then the backend applies the database migrations and
creates the administrator from `ADMIN_EMAIL`, and Traefik obtains the certificate on the first
request to your domain.

`docker compose ps` should show every service as `healthy` or `running`.

Open `https://app.example.com/login` and sign in as the administrator. The `ADMIN_*` variables
are ignored from now on (an admin exists), so you can remove `ADMIN_PASSWORD` from `.env`.

## 5. Email deliverability

Password resets and confirmations only help if they reach the inbox. With your email provider:

1. Send from a domain you control, ideally a subdomain such as `mail.example.com`, and set `MAIL_FROM` to an address on it.
2. Add the DNS records the provider gives you:
   - **SPF:** a TXT record, e.g. `v=spf1 include:<provider> ~all`.
   - **DKIM:** usually CNAME or TXT records.
   - **DMARC:** a TXT record at `_dmarc.<domain>`, e.g. `v=DMARC1; p=none; rua=mailto:dmarc@example.com`. Move to `p=quarantine` once reports look clean.
3. Send yourself a password reset and check its headers, or use [mail-tester.com](https://www.mail-tester.com).

When `MAIL_HOST` is empty the backend doesn't send email: it logs each message (and its link) and warns at startup.

## 6. Backups

The `backup` service dumps the database every day into `./backups` and keeps 14 days of dumps.
The dumps hold password hashes and emails, so only root can read them. They're also on the
same disk as the database, so **copy them elsewhere**. For example, a nightly job in root's
crontab (`sudo crontab -e`) with [rclone](https://rclone.org) or [restic](https://restic.net):

```cron
30 4 * * * rclone sync /home/deploy/app/backups remote:acme-backups
```

Also keep a copy of `.env` somewhere safe (a password manager): a backup can't be used without its secrets.
If you use RustFS, its files live in the `rustfs-data` volume; back that up too, or use a managed S3 service.

**Restore**, ideally rehearsed once on a test server:

```bash
deploy/backup/restore.sh backups/acme-20260101T000000Z.dump
```

The script asks for confirmation, stops the app, restores the dump, and starts the app again.

## 7. Updates

Building on the server:

```bash
git pull
docker compose up -d --build
```

The backend runs any new Flyway migrations as it starts. Before an update that includes migrations, make a fresh dump:

```bash
docker compose exec backup sh -c 'pg_dump -Fc > /backups/before-update.dump'
```

Migrations only move forward. **Rolling back the code doesn't roll back the database**, so write
migrations the previous version can live with (add columns before removing old ones), or
restore the dump.

Keep the base images current. Once a month:

```bash
docker compose build --pull && docker compose up -d
```

## 8. Monitoring

- `docker compose ps` shows each container's health.
- `docker compose logs --since 1h backend` shows recent logs. In production the backend writes JSON lines (ECS format), ready for Loki, Elasticsearch or any log service. Docker keeps up to 5 × 10 MB of logs per container.
- For outside uptime checks, watch:
  - `https://app.example.com/healthz`: the web app (doesn't touch the API);
  - `https://app.example.com/api/v1/public/config`: the API, through Traefik.
- The backend's own health endpoints (`/actuator/health/liveness`, `/readiness`) aren't routed by Traefik; Docker uses them for the container health check.

## 9. Images built by CI (optional)

Instead of building on the server, let GitHub Actions build images.

1. Push a version tag:

   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```

   The CI workflow publishes `ghcr.io/<owner>/<repo>-backend:1.0.0` and `ghcr.io/<owner>/<repo>-frontend:1.0.0`.
2. On the server, add to `.env`:

   ```bash
   BACKEND_IMAGE=ghcr.io/<owner>/<repo>-backend
   FRONTEND_IMAGE=ghcr.io/<owner>/<repo>-frontend
   APP_VERSION=1.0.0
   ```

3. If the repository is private, log in once with a token that has `read:packages`:

   ```bash
   docker login ghcr.io -u <github-user>
   ```

4. Deploy:

   ```bash
   docker compose pull && docker compose up -d
   ```

To roll back, set `APP_VERSION` to the previous tag and run the same command, keeping the database caveat above in mind.

The frontend image is built with an empty `VITE_API_URL`, so it calls the API on its own origin. That's what this setup needs.

## 10. Security checklist

- [ ] Secrets in `.env` are generated, not copied from `.env.example`, and `.env` is `chmod 600`.
- [ ] Only ports 22, 80 and 443 are open. PostgreSQL and RustFS aren't published.
- [ ] Registration is closed (**Administration → App settings**) if the app is invite-only.
- [ ] Administrators use strong, unique passwords, and there are as few of them as possible.
- [ ] Backups are copied off the server, and you have restored one at least once.
- [ ] Base images are rebuilt with `--pull` regularly.

Two design limits to know about:

- **Tokens live in the browser's localStorage.** Any script running on your pages could read them. The Content-Security-Policy (`frontend/svelte.config.js`) only allows scripts from your own origin and Google's sign-in, which blocks injected scripts. Keep it strict, avoid `{@html}` with user content, and vet third-party scripts before allowing them. Refresh tokens rotate, and a stolen one that gets reused signs that session out.
- **One backend instance.** Rate limits, the token state cache and the runtime settings cache live in memory. Before running several backend replicas, move them to shared storage (for example Bucket4j with PostgreSQL or Redis).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| PostgreSQL won't start: "in 18+, these Docker images are configured to store database data in a format…" | The volume is mounted at `/var/lib/postgresql/data`. Since version 18, mount `/var/lib/postgresql`, as the compose files do. |
| Backend exits: "JWT_SECRET still holds an example value" | Generate one: `openssl rand -base64 48`. |
| Backend exits: "must be at least 32 characters (set JWT_SECRET)" | `JWT_SECRET` is missing or too short. |
| Traefik logs certificate errors (`docker logs traefik`) | DNS doesn't point at the server yet, or ports 80 and 443 are blocked. The Let's Encrypt challenge is retried on the next request to the domain. |
| 404 from Traefik | The routing file isn't in Traefik's dynamic directory, the domain inside it is still `app.example.com`, or the containers aren't on the `edge` network (`docker network inspect edge`). |
| 502 / 504 from Traefik | The containers are unhealthy (`docker compose ps`), or they aren't on `edge`. |
| Pages take seconds, then render without data; frontend logs "GET /api/v1/public/config failed" | The frontend's server can't reach the API: check `API_INTERNAL_URL` and that the backend is healthy. |
| Email links point at localhost | `APP_URL` isn't set. `compose.prod.yaml` sets it from `APP_DOMAIN`; with `bootRun`, set it in `.env`. |
| Emails don't arrive | Look for "Giving up on … mail" in the backend logs, check the SMTP settings, then your provider's activity log and SPF/DKIM. |
| Backend logs "Storage not ready" and exits | RustFS isn't running (`deploy/compose.storage.yaml` missing from `COMPOSE_FILE`) or the keys differ. `STORAGE_ACCESS_KEY` and `STORAGE_SECRET_KEY` configure both services. |
| Everyone hits rate limits together | Another proxy (Cloudflare, a load balancer) sits in front of Traefik, so every request seems to come from it. Tell Traefik to trust that proxy's forwarded headers: [`entryPoints.websecure.forwardedHeaders.trustedIPs`](https://doc.traefik.io/traefik/routing/entrypoints/#forwarded-headers). |
