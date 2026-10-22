# CLAUDE.md

Notes for AI-assisted work on this repository. Keep them true: when the code changes, update
this file in the same commit. The human-facing docs live in `README.md` and `docs/`.

## Layout

```
backend/    Spring Boot 4 + Kotlin, JDK 25, Gradle (package tech.astro.liftoff)
frontend/   SvelteKit 2 + Svelte 5 runes, Tailwind 4, shadcn-svelte, Paraglide
deploy/     Traefik config, backup/restore scripts      compose.yaml, compose.prod.yaml at the root
docs/       CONFIGURATION, DEPLOYMENT, ARCHITECTURE, SHOP, MOBILE
```

## Commands

```bash
docker compose up -d                                    # PostgreSQL 18 + Mailpit (needed by bootRun)
cd backend && ./gradlew bootRun                         # API :8080, reads ../.env
cd backend && ./gradlew build                           # all tests (Testcontainers needs Docker)
cd frontend && npm run dev                              # :5173, proxies /api to :8080
cd frontend && npm run lint && npm run check && npm run test:unit -- --run && npm run build
cd frontend && npm run gen:api                          # after any API change, backend running
docker compose --profile app up -d --build              # whole stack behind Traefik on :8000
cd frontend && npm run test:e2e                         # against :8000 (start it with RATE_LIMIT_ENABLED=false)
```

Before saying a change is done, run the checks for the side you touched: `./gradlew build`, or
`npm run lint && npm run check && npm run test:unit -- --run`.

## Backend conventions

- **Feature modules:** `auth`, `user`, `admin`, `audit`, `settings`, `mail`, `storage`, `catalog` and `order` (the shop, see `docs/SHOP.md`), `note` (example), and `shared` for cross-cutting code. Inside a module: `controller/ dto/ model/ repository/ service/`.
- **Shop:** money is `Long` minor units (`...Cents`), never floating point. Stock only changes through `ProductRepository.takeStock/returnStock` (atomic conditional updates). Orders copy product names, prices and the address: never read live product data to display an order. Store rules (currency, shipping) come from `PricingService`. Store search goes through `ProductSearchService` (SQL via `JdbcClient`, see `docs/SHOP.md`); never interpolate user input into its SQL: parse it with `SearchTerms` and bind parameters.
- **Schema:** Flyway migrations (`db/migration/V<n>__name.sql`). Never edit an applied migration. Hibernate only validates (`ddl-auto: validate`).
- **Errors:** throw the `AppException` subclasses (`shared/error/AppException.kt`) with a stable `code` like `note.not_found`. Responses are RFC 9457 problem documents. A new user-facing code also needs a message in the frontend's `errorMessage()` (`src/lib/api/errors.ts`).
- **Security:**
  - `/api/v1/admin/**` requires ADMIN; the role hierarchy is ADMIN > USER.
  - Methods can use `@PreAuthorize`.
  - Inject `CurrentUser` into controllers; never trust user ids from request bodies.
  - Scope owned data by owner and answer 404 for other people's data (see `NoteService`).
- **Invalidating tokens:** call `user.bumpSecurityVersion()` and evict `UserSecurityStateCache`, as `AdminUserService` does.
- **Audit:** record security and admin actions with `AuditService.record(...)` inside the same transaction.
- **Email:** `AccountMailer` publishes events that are sent after commit. Texts live in `resources/i18n/messages*.properties` (fr, en, ar) and must exist in all three. Apostrophes are doubled (`''`) because of `MessageFormat`.
- **Config:** typed in `AppProperties`; each value maps to an environment variable in `application.yml`. A new variable must also be documented in `docs/CONFIGURATION.md` and added to `.env.example`, `compose.yaml` and `compose.prod.yaml` as relevant.
- **Tests:**
  - integration tests extend `support/IntegrationTest` (MockMvc, PostgreSQL 18 Testcontainer, captured mail);
  - unit tests use MockK;
  - the test admin is `admin@liftoff.test` / `Admin-Password-1`.
- **Jackson 3** (`tools.jackson`) is the API's JSON library. The Jackson 2 Kotlin module is only there so springdoc marks non-null fields as required.

## Frontend conventions

- **Svelte 5 runes only:** `$state`, `$derived`, `$effect`, `$props`. Use `$app/state`, never `$app/stores` (ESLint enforces it).
- **Rendering:** `(marketing)` (the storefront) and `(auth)` pages are server-rendered, except `/cart` and `/checkout` (`ssr = false`: the cart lives in localStorage). `(app)` is browser-only (`ssr = false`), because tokens live in localStorage. No `+page.server.ts` and no form actions, so the static SPA build keeps working.
- **API calls:**
  - always through `api` from `$lib/api/client` (typed by `schema.d.ts`, generated: don't edit it);
  - server-rendered store pages use `storeApi(fetch, url)` + `loadOrError()` from `$lib/shop/store-api` instead (works during SSR);
  - `call()` in components;
  - `callForPage()` in `load` functions, so failures become SvelteKit error pages;
  - API data in `load`, with `depends('app:<key>')` plus `invalidate('app:<key>')` after mutations.
- **Session:** `$lib/auth/session.svelte` (`session.user`, `session.isAdmin`, `signIn`, `signOut`). Don't read tokens directly.
- **Forms:** `createForm` from `$lib/forms/form.svelte` with zod schemas built from `$lib/forms/schemas`, plus the shadcn `Field` components. For edit forms, `initial: () => ({...})` and render inside `{#key id}`.
- **Theme and SEO:** the look comes from `theme.*`/`store.*` settings read with `storeSettings()` (`$lib/theme/store-settings`) and applied by `themeCss()` in the root layout; use theme tokens (`bg-primary`, `rounded-lg`...), never fixed brand colours. Every public page renders `<Seo>` (title, description, canonical, optional `jsonLd`); signed-in pages are noindex automatically. Raw HTML is only used for the theme style tag and JSON-LD, both built in TS (`themeStyleTag`, `jsonLdTag`) from validated or escaped values.
- **Text:** every visible string is a Paraglide message (`m.key()`), added to all three files in `frontend/messages/` (fr is the base language, ar is right-to-left). Plurals use the `notes_count` format. Format money with `formatMoney(minor, currency)`; the store currency comes from `shopSettings(config.settings)`.
- **Layout:** logical CSS only (`ms-/me-/ps-/pe-/start-/end-/text-start`). `npm run lint` fails on `left-*`, `ml-*`, `text-left`, … Flip arrow icons with `rtl:rotate-180`. Format dates and numbers with `$lib/i18n/format`.
- **UI:**
  - shadcn-svelte components in `$lib/components/ui` (add new ones with the CLI);
  - app components in `$lib/components/app`;
  - icons from `@lucide/svelte/icons/<name>`;
  - toasts with `svelte-sonner`.
- **Navigation:** the sidebar comes from `$lib/config/nav.ts`; admin-only groups have `adminOnly: true`.
- **Security:** keep the CSP in `svelte.config.js` strict. No inline scripts (the theme script is `static/theme-init.js`), no `{@html}` with user content.

## The example module

`notes` exists to be copied. Its code outside its own folders is tagged with `example:notes`
markers, so `node scripts/init.mjs --remove-example` can strip it. Keep those markers when
editing nearby code.

## Gotchas

- PostgreSQL 18 images keep data in `/var/lib/postgresql` (not `.../data`).
- `.env` is imported by Spring as a `.properties` file: no quotes, no comments after values.
- Svelte's preprocessor treats a literal `<style>`/`</script>` inside a template string as markup: build such strings in a `.ts` file.
- openapi-fetch reads `content-length`/`content-type` on responses; `hooks.server.ts` lets SvelteKit serialize them (`filterSerializedResponseHeaders`), or SSR store loads fail.
- SvelteKit's server-side `fetch` adds an `Origin` header, which the backend's CORS check rejects. `hooks.server.ts` calls the platform `fetch` for API requests on purpose.
- Group layouts without pages lose their parent's `data` type in generated `$types`: add a page before relying on it.
- To stop a background `bootRun`, use a bracket pattern such as `pkill -f "[A]pplicationKt"`: a plain pattern also matches your own shell.
