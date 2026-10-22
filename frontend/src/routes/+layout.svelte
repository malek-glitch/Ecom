<script lang="ts">
	import '../app.css';
	import { ModeWatcher } from 'mode-watcher';
	import { Toaster } from '$lib/components/ui/sonner';
	import { textDirection } from '$lib/i18n/locale';
	import { getLocale } from '$lib/paraglide/runtime';
	import { storeSettings } from '$lib/theme/store-settings';
	import { themeCss, themeStyleTag } from '$lib/theme/theme';

	let { data, children } = $props();

	// The admin's theme (Administration → Appearance), as CSS variables on top of app.css.
	const theme = $derived(themeCss(storeSettings(data.config.settings)));

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

<svelte:head>
	<!-- Only fixed values and a validated #rrggbb colour (lib/theme/theme.ts), never free text. -->
	<!-- eslint-disable-next-line svelte/no-at-html-tags -->
	{@html themeStyleTag(theme)}
</svelte:head>

<!-- static/theme-init.js applies the saved theme before first paint (mode-watcher's inline script would break the CSP). -->
<ModeWatcher disableHeadScriptInjection />
<Toaster position="top-center" dir={textDirection()} />

{@render children()}
