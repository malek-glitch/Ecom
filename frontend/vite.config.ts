import { paraglideVitePlugin } from '@inlang/paraglide-js';
import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vitest/config';

const spa = process.env.ADAPTER === 'static';
// Where `npm run dev` forwards /api/* (the Spring Boot backend).
const apiTarget = process.env.API_PROXY_TARGET ?? 'http://localhost:8080';
const proxy = { '/api': { target: apiTarget, xfwd: true } };

export default defineConfig({
	define: {
		__SPA__: JSON.stringify(spa),
	},
	plugins: [
		tailwindcss(),
		paraglideVitePlugin({
			project: './project.inlang',
			outdir: './src/lib/paraglide',
			// The cookie is readable on the server too, so server-rendered pages use the chosen language.
			strategy: ['cookie', 'preferredLanguage', 'baseLocale'],
		}),
		sveltekit(),
	],
	server: { port: 5173, proxy },
	preview: { port: 4173, proxy },
	test: {
		include: ['src/**/*.test.ts'],
		environment: 'node',
	},
});
