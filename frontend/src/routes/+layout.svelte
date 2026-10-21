<script lang="ts">
	import '../app.css';
	import { ModeWatcher } from 'mode-watcher';
	import { Toaster } from '$lib/components/ui/sonner';
	import { textDirection } from '$lib/i18n/locale';
	import { getLocale } from '$lib/paraglide/runtime';

	let { children } = $props();

	$effect(() => {
		// The server sets <html lang dir> (hooks.server.ts); the SPA build has no server to do it.
		if (__SPA__) {
			document.documentElement.lang = getLocale();
			document.documentElement.dir = textDirection();
		}
		// Lets end-to-end tests wait until forms are interactive.
		document.documentElement.dataset.hydrated = '';
	});
</script>

<!-- static/theme-init.js applies the saved theme before first paint (mode-watcher's inline script would break the CSP). -->
<ModeWatcher disableHeadScriptInjection />
<Toaster position="top-center" dir={textDirection()} />

{@render children()}
