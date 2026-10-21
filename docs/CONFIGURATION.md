# Configuration

All configuration comes from environment variables, kept in `.env` at the repository root.

| How you run it | Where the variables come from |
|---|---|
| Docker Compose | `.env`, read by Compose, which passes each service only the variables it needs |
| `./gradlew bootRun` | `../.env`, imported by the backend (real environment variables win) |
| `npm run dev` | the shell; the frontend needs no variables in development |

Format rules for `.env`:

- One `KEY=value` per line.
- No quotes, and no `#` comment after a value: Spring reads the file as a `.properties` file.
- Start from `.env.example`, or run `node scripts/init.mjs`, which also generates the secrets.

## Variables

### App

| Variable | Default | Used by | Purpose |
|---|---|---|---|
| `APP_NAME` | `Liftoff` | backend | Display name, in the app and in emails. |
| `APP_URL` | `http://localhost:5173` | backend | Public URL of the web app, used to build email links. Compose sets it for you (`http://localhost:8000`, or `https://APP_DOMAIN` in production). |
| `APP_DOMAIN` | | `compose.prod.yaml` | Your domain, e.g. `app.example.com`. `APP_URL` and `ORIGIN` derive from it. Put the same domain in `deploy/traefik/liftoff.yml`, which Traefik reads. |
| `COMPOSE_PROJECT_NAME` | `liftoff` | Compose | Prefix for container and volume names. |

### Database

| Variable | Default | Used by | Purpose |
|---|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `liftoff` | postgres container | Created on the first start of an empty volume. Changing them later doesn't change an existing database. |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/liftoff` | backend (`bootRun`) | JDBC URL. Compose builds it from the `POSTGRES_*` values. |
| `DATABASE_USER`, `DATABASE_PASSWORD` | `liftoff` | backend (`bootRun`) | Credentials; keep them equal to `POSTGRES_USER` / `POSTGRES_PASSWORD`. |
| `DATABASE_POOL_SIZE` | `10` | backend | Maximum JDBC connections. |
| `POSTGRES_PORT` | `5432` | `compose.yaml` | Host port in development. |

### Authentication

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | none, **required** | Signs access tokens. At least 32 characters. Generate with `openssl rand -base64 48`. Changing it is invisible to users: their apps get new access tokens with their refresh tokens. In production, example values are refused at startup. |
| `JWT_ISSUER` | `liftoff` | `iss` claim of access tokens. |
| `JWT_ACCESS_TTL` | `15m` | Lifetime of an access token. |
| `JWT_REFRESH_TTL` | `30d` | A session ends after this long without use; each use extends it. |
| `AUTH_REQUIRE_VERIFIED_EMAIL` | `true` | Users must confirm their email before signing in. With `false`, new accounts are signed in at once and see a reminder banner. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | | Creates the first administrator at startup, or promotes an existing account with that email. Only applies while there is no admin, so it's safe to leave set. |
| `GOOGLE_CLIENT_ID` | empty | Turns on Google sign-in (see [Google sign-in](#google-sign-in)). Empty hides the button. |

Durations use Spring's format: `30s`, `15m`, `2h`, `30d`.

### Email

| Variable | Default | Purpose |
|---|---|---|
| `MAIL_HOST` | empty | SMTP server. **Empty means emails are only written to the log**, which is handy for local runs without Mailpit. |
| `MAIL_PORT` | `1025` (dev), `587` (prod) | SMTP port. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | empty | SMTP credentials. |
| `MAIL_SMTP_AUTH` | `false` (dev), `true` (prod) | Send credentials. |
| `MAIL_SMTP_STARTTLS` | `false` (dev), `true` (prod) | Upgrade to TLS (port 587). |
| `MAIL_FROM` | `Liftoff <no-reply@liftoff.local>` | Sender. Use an address on a domain you've set up for sending ([deliverability](DEPLOYMENT.md#email-deliverability)). |

### File storage (optional)

| Variable | Default | Purpose |
|---|---|---|
| `STORAGE_ENABLED` | `false` | Turns on uploads (profile photos). Off: the upload buttons are hidden. |
| `STORAGE_ENDPOINT` | `http://localhost:9000` | S3 endpoint. Empty means AWS S3 itself. |
| `STORAGE_REGION` | `us-east-1` | Region; most S3-compatible servers accept any value. |
| `STORAGE_BUCKET` | `liftoff` | Bucket, created at startup if missing. |
| `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY` | | Credentials. With the bundled RustFS, these are also its root credentials. |
| `STORAGE_PUBLIC_BASE_URL` | empty | Empty: the API streams files at `/api/v1/files/public/...`, and the bucket stays private. Set it to serve files from a CDN or a public bucket instead. |

### Other

| Variable | Default | Purpose |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | empty | Comma-separated origins allowed to call the API from a browser on another origin. Not needed when the API and app share a domain (the default setup). Capacitor needs `capacitor://localhost,https://localhost`. |
| `RATE_LIMIT_ENABLED` | `true` | Per-IP limits on sign-in, sign-up, refresh and email endpoints. |
| `SPRING_PROFILES_ACTIVE` | | `prod` in `compose.prod.yaml`. See [Profiles](#profiles). |

### Frontend

| Variable | When | Default | Purpose |
|---|---|---|---|
| `ORIGIN` | runtime | | Public URL of the app (`https://app.example.com`). SvelteKit needs it behind a proxy. Compose sets it. |
| `API_INTERNAL_URL` | runtime | `http://localhost:8080` | How the frontend's *server* reaches the API, for server-rendered pages. Compose sets `http://backend:8080`. |
| `PORT` | runtime | `3000` | Port of the Node server. |
| `VITE_API_URL` | build time | empty | Where the *browser* sends API calls. Empty means the same origin (`/api/...`). Set it only for builds served from another origin, such as a mobile app or a separate API domain. |
| `API_PROXY_TARGET` | `npm run dev` | `http://localhost:8080` | Where the dev server forwards `/api`. |

### Docker images and resources (`compose.prod.yaml`)

| Variable | Default | Purpose |
|---|---|---|
| `BACKEND_IMAGE`, `FRONTEND_IMAGE` | `liftoff-backend`, `liftoff-frontend` | Image names. Point them at the images CI publishes (`ghcr.io/<owner>/<repo>-backend`) to deploy without building on the server. |
| `APP_VERSION` | `latest` | Image tag, e.g. `1.4.0`. |
| `BACKEND_MEMORY`, `FRONTEND_MEMORY` | `1g`, `512m` | Container memory limits. The JVM heap is 75% of the backend's limit. |
| `BACKUP_RETENTION_DAYS` | `14` | Days of database dumps to keep in `./backups`. |
| `BACKUP_INTERVAL_SECONDS` | `86400` | Time between dumps. |

### Development ports (`compose.yaml`)

| Variable | Default |
|---|---|
| `APP_PORT` (full stack behind Traefik) | `8000` |
| `MAILPIT_SMTP_PORT`, `MAILPIT_UI_PORT` | `1025`, `8025` |
| `RUSTFS_PORT`, `RUSTFS_CONSOLE_PORT` | `9000`, `9001` |

### End-to-end tests

`E2E_BASE_URL` (default `http://localhost:8000`) and `E2E_MAILPIT_URL` (default `http://localhost:8025`) tell the tests where the stack runs.

`E2E_ADMIN_EMAIL` and `E2E_ADMIN_PASSWORD` default to the values in `.env.example`.

`E2E_CHROMIUM_PATH` (optional) runs the tests on a Chromium that's already installed.

## Advanced settings

These rarely change, so they have no line in `.env.example`. Set them like any other variable.
Spring maps `app.auth.password-reset-ttl` to `APP_AUTH_PASSWORDRESETTTL`: uppercase it, turn dots into underscores and drop the dashes.

| Setting (environment variable) | Default | Purpose |
|---|---|---|
| `app.auth.refresh-reuse-grace` (`APP_AUTH_REFRESHREUSEGRACE`) | `10s` | How long an already-used refresh token is still accepted, for two tabs refreshing at once. After that, reuse counts as theft and signs out the session. |
| `app.auth.email-verification-ttl` (`APP_AUTH_EMAILVERIFICATIONTTL`) | `48h` | Lifetime of confirmation links. |
| `app.auth.password-reset-ttl` (`APP_AUTH_PASSWORDRESETTTL`) | `1h` | Lifetime of password reset links. |
| `app.auth.invitation-ttl` (`APP_AUTH_INVITATIONTTL`) | `7d` | Lifetime of invitation links. |
| `app.storage.create-bucket` (`APP_STORAGE_CREATEBUCKET`) | `true` | Create the bucket at startup. Turn it off when the key can't create buckets. |
| `app.storage.max-avatar-size` (`APP_STORAGE_MAXAVATARSIZE`) | `5MB` | Largest accepted profile photo. |
| `app.rate-limit.policies.<name>.capacity` / `.period` (`APP_RATELIMIT_POLICIES_LOGIN_CAPACITY`, `APP_RATELIMIT_POLICIES_LOGIN_PERIOD`) | see below | Override a rate-limit policy. |
| `app.audit.retention` (`APP_AUDIT_RETENTION`) | `365d` | Audit events older than this are deleted every night. |
| `app.maintenance.cron` (`APP_MAINTENANCE_CRON`) | `0 17 3 * * *` | When the nightly cleanup runs: expired sessions, used email links, old audit events. |

Default rate-limit policies, per client IP:

| Policy | Limit | Endpoints |
|---|---|---|
| `login` | 10 per minute | sign-in, email confirmation, password reset, password change |
| `register` | 10 per 10 minutes | sign-up |
| `email` | 5 per 15 minutes | "send the link again", "forgot password" |
| `refresh` | 60 per minute | token refresh |
| `google` | 20 per minute | Google sign-in |
| `upload` | 20 per 10 minutes | profile photo |

## Runtime settings

Administrators change these in **Administration → App settings**, without a redeploy.
A setting nobody has changed uses its default.

| Key | Default | Effect |
|---|---|---|
| `auth.registrationEnabled` | `true` | Whether new people can create an account. Turn it off for an invite-only app. |
| `app.announcement` | empty | A banner shown to every signed-in user. |
| `app.supportEmail` | empty | Contact address in the public footer. |

To add your own, see [Add a runtime setting](ARCHITECTURE.md#add-a-runtime-setting).

## Profiles

Without a profile, the backend runs in development mode: Swagger UI is on and logs are plain text.

`SPRING_PROFILES_ACTIVE=prod`, which `compose.prod.yaml` sets:

- refuses to start with an example `JWT_SECRET`;
- warns when `MAIL_HOST` is empty or `APP_URL` points to localhost;
- turns off Swagger UI and the OpenAPI document;
- writes JSON logs (ECS format), one line per event;
- trusts `X-Forwarded-*` headers from private-network proxies, so client IPs (used by rate limits and the audit log) are real.

## Secrets

Generate each secret, and never reuse the development values in production:

```bash
openssl rand -base64 48    # JWT_SECRET
openssl rand -base64 24    # passwords: POSTGRES_PASSWORD, STORAGE_SECRET_KEY, ...
```

`node scripts/init.mjs` writes a `.env` with all of them generated. `.env` is ignored by git.
In production, keep its permissions tight: `chmod 600 .env`.

## Google sign-in

1. In the [Google Cloud console](https://console.cloud.google.com/), select or create a project.
2. Go to **Google Auth Platform → Branding** (the OAuth consent screen). Fill in the app name, the support email and your domain.
3. Go to **Clients → Create client**:
   - **Application type:** Web application.
   - **Authorized JavaScript origins:** every URL the app runs on, e.g. `http://localhost:5173`, `http://localhost:8000` and `https://app.example.com`.
   - **Redirect URIs:** none. The button uses a popup that returns an ID token.
4. Copy the client ID into `GOOGLE_CLIENT_ID` and restart the backend.

The "Continue with Google" button then appears on the sign-in and sign-up pages.

The backend checks each Google ID token's signature, issuer, audience and expiry, and requires a verified Google email address. Signing in with Google:

- creates an account if none exists, unless registration is closed;
- links to an existing account with the same email. If that account's email wasn't confirmed yet, its password is removed and its sessions are ended, so whoever registered the address first can't keep access.

The Content-Security-Policy in `frontend/svelte.config.js` already allows Google's script.

## SMTP providers

Use port 587 with STARTTLS (`MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`) on any of these:

| Provider | `MAIL_HOST` | `MAIL_USERNAME` / `MAIL_PASSWORD` |
|---|---|---|
| Amazon SES | `email-smtp.<region>.amazonaws.com` | SES SMTP credentials (not your AWS keys) |
| Brevo | `smtp-relay.brevo.com` | your login / an SMTP key |
| Postmark | `smtp.postmarkapp.com` | the server API token for both |
| Mailgun | `smtp.mailgun.org` | the domain's SMTP credentials |
| Resend | `smtp.resend.com` | `resend` / an API key |
| Google Workspace | `smtp.gmail.com` | the address / an app password (low volumes only) |

Emails are sent in the background after the request's transaction commits. A send is attempted up to 3 times, then the failure is logged; the user can ask for the link again.

## S3-compatible storage

`STORAGE_ENABLED=true` turns on profile photos. Any S3-compatible service works:

| Service | `STORAGE_ENDPOINT` | `STORAGE_REGION` | Notes |
|---|---|---|---|
| RustFS (bundled) | `http://rustfs:9000` in containers, `http://localhost:9000` with `bootRun` | `us-east-1` | `--profile storage` in development, `deploy/compose.storage.yaml` in production. The keys are the RustFS credentials. |
| AWS S3 | empty | your bucket's region | An IAM user allowed `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` and `s3:ListBucket` on the bucket. |
| Cloudflare R2 | `https://<account-id>.r2.cloudflarestorage.com` | `auto` | Create the bucket in the dashboard and set `APP_STORAGE_CREATEBUCKET=false` if the token can't create buckets. |
| Hetzner Object Storage | `https://<location>.your-objectstorage.com` | the location, e.g. `fsn1` | |
| Backblaze B2 | `https://s3.<region>.backblazeb2.com` | the region, e.g. `eu-central-003` | |

Files are private by default and streamed through the API, so nothing in the bucket needs to be public.
To serve them from a CDN instead, make the bucket (or a CDN in front of it) public and set
`STORAGE_PUBLIC_BASE_URL` to its base URL. File URLs are then built from that base.

Uploaded profile photos are checked for their actual format (JPEG, PNG, WebP or GIF), size and dimensions, then cropped to a square and resized to 512 px.
