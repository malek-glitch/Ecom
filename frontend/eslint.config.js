import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import svelte from 'eslint-plugin-svelte';
import globals from 'globals';
import ts from 'typescript-eslint';
import svelteConfig from './svelte.config.js';

export default ts.config(
	{
		ignores: [
			'.svelte-kit/**',
			'build/**',
			'test-results/**',
			'playwright-report/**',
			// Generated, or copied from shadcn-svelte (re-add with its CLI rather than editing by hand).
			'src/lib/paraglide/**',
			'src/lib/api/schema.d.ts',
			'src/lib/components/ui/**',
		],
	},
	js.configs.recommended,
	...ts.configs.recommended,
	...svelte.configs.recommended,
	prettier,
	...svelte.configs.prettier,
	{
		languageOptions: { globals: { ...globals.browser, ...globals.node } },
		rules: {
			// TypeScript already reports undefined names, and knows about ambient types.
			'no-undef': 'off',
			'@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
			// Links are plain paths because the app is served from the domain root (docs/DEPLOYMENT.md).
			// Deploying under a sub-path? Turn this on and wrap links in resolve() from $app/paths.
			'svelte/no-navigation-without-resolve': 'off',
			'no-restricted-imports': [
				'error',
				{
					paths: [
						{ name: '$app/stores', message: 'Use $app/state (stores are deprecated and removed in SvelteKit 3).' },
						{ name: 'lucide-svelte', message: 'Import icons from @lucide/svelte/icons/<name>.' },
					],
					patterns: [{ group: ['lucide-svelte/*'], message: 'Import icons from @lucide/svelte/icons/<name>.' }],
				},
			],
		},
	},
	{
		files: ['**/*.svelte', '**/*.svelte.ts', '**/*.svelte.js'],
		languageOptions: {
			parserOptions: { projectService: true, extraFileExtensions: ['.svelte'], parser: ts.parser, svelteConfig },
		},
	},
);
