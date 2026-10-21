// Regenerates src/lib/api/schema.d.ts from the backend's OpenAPI spec.
// Run it after changing a backend DTO or endpoint (with the backend running):
//   npm run gen:api                       # http://localhost:8080/v3/api-docs
//   npm run gen:api -- ./openapi.json     # or any URL / file
import { execFileSync } from 'node:child_process';

const source = process.argv[2] ?? process.env.API_DOCS_URL ?? 'http://localhost:8080/v3/api-docs';
execFileSync('npx', ['--yes', 'openapi-typescript@7.13.0', source, '--output', 'src/lib/api/schema.d.ts'], {
	stdio: 'inherit',
	shell: process.platform === 'win32',
});
