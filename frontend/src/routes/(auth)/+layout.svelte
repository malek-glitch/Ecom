<script lang="ts">
	import LocaleSwitcher from '$lib/components/app/LocaleSwitcher.svelte';
	import Logo from '$lib/components/app/Logo.svelte';
	import ThemeToggle from '$lib/components/app/ThemeToggle.svelte';
	import { m } from '$lib/paraglide/messages';

	let { data, children } = $props();
</script>

<div class="flex min-h-svh flex-col bg-muted/40">
	<header class="flex items-center justify-between px-4 py-3 sm:px-6">
		<a href="/" class="rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
			<Logo name={data.config.appName} />
		</a>
		<div class="flex items-center gap-1">
			<LocaleSwitcher />
			<ThemeToggle />
		</div>
	</header>
	<main id="main" class="flex flex-1 items-center justify-center px-4 py-8">
		<div class="w-full max-w-sm">
			{@render children()}
		</div>
	</main>
	<footer class="px-4 py-6 text-center text-xs text-muted-foreground">
		{m.footer_rights({ year: new Date().getFullYear(), app: data.config.appName })}
	</footer>
</div>
