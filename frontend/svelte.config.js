import adapterNode from '@sveltejs/adapter-node';
import adapterStatic from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/**
 * Two build targets from one codebase:
 *  - `npm run build`      Node server (SSR for public pages), the default deployment.
 *  - `npm run build:spa`  Static single-page app (e.g. to wrap with Capacitor, see docs/MOBILE.md).
 */
const spa = process.env.ADAPTER === 'static';
const apiOrigin = process.env.VITE_API_URL ? new URL(process.env.VITE_API_URL).origin : null;

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	compilerOptions: {
		// Runes mode for all app code; dependencies keep their own mode.
		runes: ({ filename }) => (filename.split(/[/\\]/).includes('node_modules') ? undefined : true),
	},
	kit: {
		adapter: spa
			? adapterStatic({ pages: 'build', assets: 'build', fallback: 'index.html', precompress: false })
			: adapterNode({ precompress: true }),
		// Absolute asset URLs: the SPA's fallback page is served for every route, where
		// relative ones (./theme-init.js) would resolve against the wrong folder.
		paths: { relative: false },
		// Tokens live in the browser, so a strict CSP is the main defence against XSS stealing them.
		csp: {
			mode: 'auto',
			directives: {
				'default-src': ['self'],
				'script-src': ['self', 'https://accounts.google.com/gsi/client'],
				'style-src': ['self', 'unsafe-inline', 'https://accounts.google.com/gsi/style'],
				'img-src': ['self', 'data:', 'blob:', 'https:'],
				'font-src': ['self', 'data:'],
				'connect-src': ['self', 'https://accounts.google.com/gsi/', ...(apiOrigin ? [apiOrigin] : [])],
				'frame-src': ['https://accounts.google.com/gsi/'],
				'object-src': ['none'],
				'base-uri': ['self'],
				'form-action': ['self'],
			},
		},
	},
};

export default config;
