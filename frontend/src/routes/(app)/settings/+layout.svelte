<script lang="ts">
	import { page } from '$app/state';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import { m } from '$lib/paraglide/messages';
	import { cn } from '$lib/utils';

	let { children } = $props();

	const tabs = [
		{ href: '/settings/profile', label: m.settings_profile },
		{ href: '/settings/security', label: m.settings_security },
	];
</script>

<PageHeader title={m.settings_title()} description={m.settings_subtitle()} />

<nav class="mb-6 flex gap-1 border-b" aria-label={m.settings_title()}>
	{#each tabs as tab (tab.href)}
		{@const active = page.url.pathname === tab.href}
		<a
			href={tab.href}
			aria-current={active ? 'page' : undefined}
			class={cn(
				'-mb-px border-b-2 px-3 py-2 text-sm font-medium transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
				active ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
			)}
		>
			{tab.label()}
		</a>
	{/each}
</nav>

<div class="max-w-3xl space-y-6">
	{@render children()}
</div>
