// Fails when components use physical left/right Tailwind utilities, which don't mirror in
// right-to-left languages (Arabic). Use logical ones: ms/me, ps/pe, start/end, text-start/end,
// border-s/e, rounded-s/e. Intentional exceptions: add "rtl-ok" in a comment on the same line.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const VALUE = String.raw`-(?:\d[\w./]*|\[[^\]\s]+\]|px|auto|full|none)`;
const PHYSICAL = new RegExp(
	String.raw`(?<![\w\[=-])(?:[a-z-]+:)*-?(?:(?:m[lr]|p[lr]|left|right|inset-[lr]|scroll-m[lr]|scroll-p[lr])${VALUE}|(?:border-[lr]|rounded-(?:[lr]|[tb][lr]))(?:${VALUE})?|text-(?:left|right)|float-(?:left|right)|clear-(?:left|right))(?![\w-])`,
	'g',
);

// Direction-neutral: centring with left-[50%] + translate-x-[-50%] looks the same either way.
const ALLOWED = new Set(['left-[50%]']);

function* files(dir) {
	for (const name of readdirSync(dir)) {
		const path = join(dir, name);
		if (statSync(path).isDirectory()) yield* files(path);
		else if (/\.(svelte|ts)$/.test(name) && !name.endsWith('.d.ts')) yield path;
	}
}

const problems = [];
for (const file of files('src')) {
	if (file.includes('paraglide')) continue;
	readFileSync(file, 'utf8')
		.split('\n')
		.forEach((line, index) => {
			if (line.includes('rtl-ok')) return;
			// only look inside string literals and class attributes
			const strings = line.match(/(["'`])(?:(?!\1).)*\1/g) ?? [];
			for (const str of strings) {
				for (const match of str.matchAll(PHYSICAL)) {
					if (ALLOWED.has(match[0])) continue;
					problems.push(`${file}:${index + 1}  ${match[0]}`);
				}
			}
		});
}

if (problems.length) {
	console.error(`Physical (non-RTL-safe) utilities found:\n${problems.join('\n')}`);
	process.exit(1);
}
console.log('RTL check passed: no physical left/right utilities.');
