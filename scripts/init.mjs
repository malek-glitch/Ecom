#!/usr/bin/env node
/**
 * Turns this template into your own project. Run it once, right after creating your repository:
 *
 *   node scripts/init.mjs --slug acme --name "Acme" --package com.acme.app
 *
 *   --slug            Lowercase id (letters, digits, dashes): Docker project, database,
 *                     bucket, browser storage, package names. Required.
 *   --name            Display name, shown in the app and in emails. Default: from the slug.
 *   --package         Kotlin base package. Default: com.example.<slug>.
 *   --remove-example  Delete the example "notes" module (API, migration, pages, messages).
 *   --dry-run         Print what would change, write nothing.
 *
 * It also creates .env from .env.example with fresh random secrets (an existing .env is kept).
 * Needs Node 22+ and nothing else, so it runs before `npm install`.
 */
import { randomBytes } from 'node:crypto';
import { existsSync, mkdirSync, readdirSync, readFileSync, renameSync, rmSync, rmdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const TEMPLATE = { name: 'Liftoff', slug: 'liftoff', pkg: 'tech.astro.liftoff', group: 'tech.astro' };
/** Where the template lives: links to it stay as they are. */
const TEMPLATE_REPOSITORY = 'malek-glitch/liftoff';

const SKIP_DIRS = new Set([
	'.git',
	'node_modules',
	'.svelte-kit',
	'build',
	'.gradle',
	'.kotlin',
	'paraglide',
	'cache',
	'test-results',
	'playwright-report',
	'backups',
	'.idea',
]);
const BINARY = /\.(png|jpe?g|gif|webp|ico|jar|woff2?|ttf|otf|zip|gz|br|pdf)$/i;
const KOTLIN_KEYWORDS = new Set(
	'as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while'.split(
		' ',
	),
);

// ---------------------------------------------------------------- arguments

const { values: args } = parseArgs({
	options: {
		slug: { type: 'string' },
		name: { type: 'string' },
		package: { type: 'string' },
		'remove-example': { type: 'boolean', default: false },
		'dry-run': { type: 'boolean', default: false },
		help: { type: 'boolean', short: 'h', default: false },
	},
});

if (args.help || !args.slug) {
	const usage = readFileSync(fileURLToPath(import.meta.url), 'utf8').split('*/')[0].replace(/^#!.*\n/, '');
	console.log(usage.replace(/^\/\*\*|^ \* ?/gm, '').trim());
	process.exit(args.help ? 0 : 1);
}

const slug = args.slug;
const name = args.name ?? slug.split('-').map((word) => word[0].toUpperCase() + word.slice(1)).join(' ');
const pkg = args.package ?? `com.example.${slug.replaceAll('-', '')}`;
const dryRun = args['dry-run'];

fail(!/^[a-z][a-z0-9]*(-[a-z0-9]+)*$/.test(slug) || slug.length > 30, '--slug: lowercase letters, digits and dashes, up to 30 characters (e.g. acme-portal).');
fail(!/^[\p{L}\p{N}][\p{L}\p{N} .-]{0,39}$/u.test(name), '--name: letters, digits, spaces, dots and dashes, up to 40 characters.');
fail(
	!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/.test(pkg) || pkg.split('.').some((part) => KOTLIN_KEYWORDS.has(part)),
	'--package: a lowercase reverse domain such as com.acme.app (no Kotlin keywords like "in" or "is").',
);
fail(
	!existsSync(join(ROOT, 'backend/src/main/kotlin', ...TEMPLATE.pkg.split('.'))),
	`This project was already initialised (backend/src/main/kotlin/${TEMPLATE.pkg.replaceAll('.', '/')} is gone).`,
);

const database = slug.replaceAll('-', '_'); // PostgreSQL names are simpler without dashes
const className = slug
	.split('-')
	.map((word) => word[0].toUpperCase() + word.slice(1))
	.join('');
const envPrefix = slug.toUpperCase().replaceAll('-', '_');

// ---------------------------------------------------------------- 1. rename inside files

const changed = [];
for (const file of walk(ROOT)) {
	const path = relative(ROOT, file);
	if (path === join('scripts', 'init.mjs') || path === '.env') continue;
	const before = readFileSync(file, 'utf8');
	let text = rename(before);
	if (args['remove-example']) text = stripExample(text);
	if (text !== before) {
		changed.push(path);
		if (!dryRun) writeFileSync(file, text);
	}
}

function rename(text) {
	const keep = '\u0000TEMPLATE_REPOSITORY\u0000';
	return text
		.replaceAll(TEMPLATE_REPOSITORY, keep)
		.replaceAll(TEMPLATE.pkg, pkg)
		.replaceAll(TEMPLATE.pkg.replaceAll('.', '/'), pkg.replaceAll('.', '/'))
		.replace(`group = "${TEMPLATE.group}"`, `group = "${pkg.split('.').slice(0, -1).join('.')}"`)
		.replaceAll('LiftoffApplication', `${className}Application`)
		.replaceAll('LIFTOFF_', `${envPrefix}_`)
		// Database and role names: underscores instead of dashes.
		.replace(/\b(POSTGRES_DB|POSTGRES_USER|DATABASE_USER)(=|:-|:)liftoff\b/g, `$1$2${database}`)
		.replace(/(postgresql:\/\/[^/\s]+\/)liftoff\b/g, `$1${database}`)
		.replaceAll(TEMPLATE.name, name)
		.replaceAll(TEMPLATE.slug, slug)
		.replaceAll(keep, TEMPLATE_REPOSITORY);
}

// ---------------------------------------------------------------- 2. move the Kotlin package

for (const sourceSet of ['main', 'test']) {
	const base = join(ROOT, 'backend/src', sourceSet, 'kotlin');
	const from = join(base, ...TEMPLATE.pkg.split('.'));
	const to = join(base, ...pkg.split('.'));
	if (from === to || !existsSync(from)) continue;
	console.log(`move  ${relative(ROOT, from)} → ${relative(ROOT, to)}`);
	if (dryRun) continue;
	// Through a temporary folder, so the new package may sit inside the old one.
	const temporary = join(base, '.init-move');
	renameSync(from, temporary);
	removeEmptyParents(dirname(from), base);
	mkdirSync(dirname(to), { recursive: true });
	renameSync(temporary, to);
}
const mainSources = join(ROOT, 'backend/src/main/kotlin', ...(dryRun ? TEMPLATE.pkg : pkg).split('.'));
if (!dryRun) renameSync(join(mainSources, 'LiftoffApplication.kt'), join(mainSources, `${className}Application.kt`));

// Files whose name carries the project name (the docs refer to them by the new name).
const routing = join(ROOT, 'deploy/traefik', `${TEMPLATE.slug}.yml`);
if (existsSync(routing) && !dryRun) renameSync(routing, join(ROOT, 'deploy/traefik', `${slug}.yml`));

// ---------------------------------------------------------------- 3. the example module

if (args['remove-example']) {
	const sources = (set) => join('backend/src', set, 'kotlin', ...(dryRun ? TEMPLATE.pkg : pkg).split('.'));
	for (const path of [
		join(sources('main'), 'note'),
		join(sources('test'), 'note'),
		'backend/src/main/resources/db/migration/V3__notes.sql',
		'frontend/src/routes/(app)/notes',
		'frontend/src/lib/components/notes',
		'frontend/tests/e2e/notes.spec.ts',
	]) {
		console.log(`delete ${path}`);
		if (!dryRun) rmSync(join(ROOT, path), { recursive: true, force: true });
	}
	for (const locale of readdirSync(join(ROOT, 'frontend/messages'))) {
		const file = join(ROOT, 'frontend/messages', locale);
		const messages = JSON.parse(readFileSync(file, 'utf8'));
		for (const key of Object.keys(messages)) {
			if (/^(notes_|dashboard_notes_)/.test(key) || key === 'nav_notes') delete messages[key];
		}
		if (!dryRun) writeFileSync(file, `${JSON.stringify(messages, null, '\t')}\n`);
	}
}

/**
 * Removes code marked as part of the example: a line ending with `[example:notes]`, or the
 * lines between `[example:notes]` and `[/example:notes]` comments on lines of their own.
 */
function stripExample(text) {
	const lines = text.split('\n');
	const kept = [];
	let skipping = false;
	for (const line of lines) {
		const marker = line.trim();
		if (/^(\/\/|<!--)\s*\[example:notes\]\s*(-->)?$/.test(marker)) skipping = true;
		else if (/^(\/\/|<!--)\s*\[\/example:notes\]\s*(-->)?$/.test(marker)) skipping = false;
		else if (!skipping && !line.includes('[example:notes]')) kept.push(line);
	}
	const result = kept.join('\n');
	// A removed block can leave two blank lines in a row, which Prettier would flag.
	return result === text ? text : result.replace(/\n[ \t]*\n([ \t]*\n)+/g, '\n\n');
}

// ---------------------------------------------------------------- 4. .env with fresh secrets

const envFile = join(ROOT, '.env');
let adminPassword = null;
if (existsSync(envFile)) {
	console.log('\n.env already exists: kept as it is (it may still use the template names and secrets).');
} else {
	const secret = (bytes) => randomBytes(bytes).toString('base64url');
	const databasePassword = secret(18);
	adminPassword = secret(12);
	const env = readFileSync(join(ROOT, '.env.example'), 'utf8')
		.replace(/^JWT_SECRET=.*$/m, `JWT_SECRET=${secret(48)}`)
		.replace(/^POSTGRES_PASSWORD=.*$/m, `POSTGRES_PASSWORD=${databasePassword}`)
		.replace(/^DATABASE_PASSWORD=.*$/m, `DATABASE_PASSWORD=${databasePassword}`)
		.replace(/^ADMIN_PASSWORD=.*$/m, `ADMIN_PASSWORD=${adminPassword}`)
		.replace(/^STORAGE_SECRET_KEY=.*$/m, `STORAGE_SECRET_KEY=${secret(24)}`);
	// .env.example was already renamed above.
	if (!dryRun) writeFileSync(envFile, env, { mode: 0o600 });
	console.log('\ncreated .env with new secrets');
}

// ---------------------------------------------------------------- summary

console.log(`\n${dryRun ? 'Would update' : 'Updated'} ${changed.length} files:`);
for (const path of changed) console.log(`  ${path}`);
console.log(`
${dryRun ? 'Dry run: nothing was written.' : `Done: this is now "${name}" (${slug}, package ${pkg}).`}

Next steps:
  docker compose up -d                          # PostgreSQL + Mailpit
  cd backend && ./gradlew bootRun               # API on http://localhost:8080
  cd frontend && npm install && npm run dev     # app on http://localhost:5173${args['remove-example'] ? '\n  cd frontend && npm run gen:api                # once the API runs: drops the notes types' : ''}
${adminPassword ? `\nFirst admin: admin@${slug}.local / ${adminPassword} (also in .env)` : ''}
Delete scripts/init.mjs whenever you like: it only runs once.`);

// ---------------------------------------------------------------- helpers

function* walk(dir) {
	for (const entry of readdirSync(dir, { withFileTypes: true })) {
		if (entry.isDirectory()) {
			if (!SKIP_DIRS.has(entry.name)) yield* walk(join(dir, entry.name));
		} else if (entry.isFile() && !BINARY.test(entry.name)) {
			yield join(dir, entry.name);
		}
	}
}

function removeEmptyParents(dir, stopAt) {
	while (dir !== stopAt && dir.startsWith(stopAt) && readdirSync(dir).length === 0) {
		rmdirSync(dir);
		dir = dirname(dir);
	}
}

function fail(condition, message) {
	if (condition) {
		console.error(`init: ${message}`);
		process.exit(1);
	}
}
