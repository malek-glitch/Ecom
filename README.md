# Liftoff

A full-stack template for selling goods online: a **Spring Boot 4 + Kotlin** API, a
**SvelteKit + Svelte 5** storefront and **PostgreSQL 18**. A catalog, cart, checkout, order
handling, accounts, administration, emails, file storage, three languages (one of them
right-to-left) and a production setup work on day one, so a new shop starts with its own products.

## What's included

**Online store** ([docs/SHOP.md](docs/SHOP.md))

- Server-rendered storefront: home page, catalog with search, categories, sorting and pagination, product pages.
- A cart that works without an account, priced by the server on every change.
- Checkout with shipping address and payment by cash on delivery or bank transfer (card payments are a documented extension).
- Stock taken atomically at checkout, so the last item can't be sold twice; cancelling restocks.
- Order history for customers, with cancellation while an order is pending.
- Admin area for products (with photos), categories and orders (paid, shipped, delivered, cancelled), plus revenue and low-stock stats.
- Order emails in the customer's language: confirmation, shipped, cancelled.
- Store currency, shipping fee, free-shipping threshold and bank details as runtime settings.
- Optional demo catalog (`SHOP_SEED_DEMO=true`).

**Accounts and security**

- Email and password sign-up with email confirmation, sign-in, password reset.
- Optional Google sign-in.
- Short-lived access tokens with rotating refresh tokens. A stolen refresh token that gets reused signs that device out.
- Users can see their active sessions and sign any of them out.
- Disabling an account, changing a role or changing a password takes effect within seconds.
- Rate limits on sign-in and email endpoints, a strict Content-Security-Policy, and consistent JSON errors (RFC 9457).

**Administration**

- Two roles: `USER` and `ADMIN`.
- Admin area: key numbers, user search, invitations by email, role changes, disabling accounts.
- An audit log of security and admin events.
- Runtime settings that need no redeploy: open or close registration, announcement banner, support email.

**Emails**

- SMTP with HTML and plain-text templates, written in each user's language.
- Sent in the background after the database commit, with retries.
- In development, Mailpit catches every email.

**Files** (optional)

- Works with any S3-compatible storage (RustFS in Docker, AWS S3, Cloudflare R2, …).
- Profile photos are checked and resized on the server.

**Languages**

- French, English and Arabic throughout, with a layout that mirrors itself for Arabic.
- Uses Paraglide for translations; each user's language choice also applies to their emails.

**Frontend**

- Built with Svelte 5 runes, Tailwind CSS 4, shadcn-svelte components, Lucide icons and dark mode.
- A typed API client generated from the backend's OpenAPI spec.
- A small runes form helper that uses zod schemas.

**Example module**

- `notes`: a private CRUD feature with search, pagination and conflict detection, built to be copied.
- Remove it with one flag.

**Operations**

- Docker images for the backend and frontend.
- Compose files for development and production; Traefik routes traffic and handles HTTPS.
- Daily database backups.
- GitHub Actions CI and Playwright end-to-end tests.

## Stack

| Layer | Choice |
|---|---|
| API | Spring Boot 4.1 (Spring Framework 7, Security 7), Kotlin 2.4, JDK 25, Gradle 9 |
| Database | PostgreSQL 18, Flyway migrations, Hibernate 7 (schema validated, never auto-generated) |
| Web app | SvelteKit 2 + Svelte 5 (runes), TypeScript, Vite 8 |
| UI | Tailwind CSS 4, shadcn-svelte (bits-ui), @lucide/svelte, mode-watcher, svelte-sonner |
| i18n | Paraglide JS 2 (fr, en, ar) |
| Tests | JUnit 6 + Testcontainers (PostgreSQL, RustFS), Vitest, Playwright |
| Infra | Docker Compose, Traefik 3, Mailpit (dev), RustFS (optional S3 storage) |

## Quick start

You need Docker, Node 24 and a JDK 25. Gradle downloads a JDK 25 by itself if none is installed.

```bash
cp .env.example .env                        # development defaults
docker compose up -d                        # PostgreSQL + Mailpit
cd backend && ./gradlew bootRun             # API on http://localhost:8080
cd frontend && npm install && npm run dev   # app on http://localhost:5173
```

Sign in as `admin@liftoff.local` with password `change-me-admin-password` (both set in `.env`).
Accounts you register get their confirmation email in Mailpit, at http://localhost:8025.

To run everything in containers instead, start the stack behind a Traefik proxy, like production:

```bash
docker compose --profile app up -d --build  # http://localhost:8000
```

Add `--profile storage` to either command to start RustFS, and set `STORAGE_ENABLED=true` in `.env`
to turn uploads on.

| What | Where |
|---|---|
| Web app (dev server) | http://localhost:5173 |
| API | http://localhost:8080 |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Mailpit inbox | http://localhost:8025 |
| RustFS console (`--profile storage`) | http://localhost:9001 |
| Full stack (`--profile app`) | http://localhost:8000 |

## Starting a new project

1. On GitHub, choose **Use this template** and create your repository. Clone it.
2. Rename the project and generate a `.env` with fresh secrets:

   ```bash
   node scripts/init.mjs --slug acme --name "Acme" --package com.acme.app
   ```

   The script renames the display name, the database and container names and the Kotlin
   package. Add `--remove-example` to delete the example notes module, or `--dry-run` to
   preview the changes first.
3. Commit, then build your first feature by following the checklist in
   [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#add-a-feature).

## Project layout

```
.
├── backend/                 Spring Boot API (Kotlin)
│   └── src/main/kotlin/tech/astro/liftoff/
│       ├── auth/            sign-in, tokens, sessions, email links, Google
│       ├── user/            accounts and the /me endpoints
│       ├── admin/           user management and stats
│       ├── audit/           audit log
│       ├── settings/        runtime settings
│       ├── mail/            email rendering and sending
│       ├── storage/         S3 storage and avatars (optional)
│       ├── catalog/         categories and products, the public catalog
│       ├── order/           cart pricing, checkout, orders, order emails
│       ├── note/            the example module
│       └── shared/          config, security, errors, web helpers
├── frontend/                SvelteKit app
│   ├── messages/            translations (fr, en, ar)
│   ├── src/lib/             API client, session, forms, i18n, components
│   ├── src/routes/          (marketing) · (auth) · (app) with admin/
│   └── tests/e2e/           Playwright tests
├── deploy/                  Traefik config, backup and restore scripts
├── docs/                    configuration, deployment, architecture, shop, mobile
├── scripts/init.mjs         renames a fresh copy of the template
├── compose.yaml             development (and the full stack with --profile app)
└── compose.prod.yaml        production
```

## Everyday commands

| Where | Command | What it does |
|---|---|---|
| `backend/` | `./gradlew bootRun` | Run the API (reads `../.env`) |
| | `./gradlew test` | Unit and integration tests (Docker needed for Testcontainers) |
| | `./gradlew build` | Tests + the runnable jar |
| `frontend/` | `npm run dev` | Dev server with hot reload, proxies `/api` to the backend |
| | `npm run check` | Type-check Svelte and TypeScript |
| | `npm run lint` / `npm run format` | Prettier, ESLint and a right-to-left CSS check |
| | `npm run test:unit` | Vitest |
| | `npm run test:e2e` | Playwright, against the stack on http://localhost:8000 |
| | `npm run gen:api` | Regenerate API types from the running backend |
| | `npm run build` / `npm run build:spa` | Node server build / static single-page build |

## Documentation

- [docs/CONFIGURATION.md](docs/CONFIGURATION.md) lists every environment variable and explains how to set up Google sign-in, SMTP and S3 storage.
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) takes you from an empty server to HTTPS in production, then covers backups, updates and troubleshooting.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) explains how the pieces fit, the auth flows, and checklists for adding a feature, a role, a setting, an email or a language.
- [docs/MOBILE.md](docs/MOBILE.md) shows how to wrap the app with Capacitor.
- [CLAUDE.md](CLAUDE.md) lists the conventions for AI-assisted work on this codebase.
