<script lang="ts">
	import Rocket from '@lucide/svelte/icons/rocket';
	import { page } from '$app/state';
	import { storeSettings } from '$lib/theme/store-settings';
	import { cn } from '$lib/utils';

	/**
	 * The store's logo image (setting store.logoUrl), or an icon with the name. `logoUrl` overrides
	 * the setting (the appearance preview); null forces the icon.
	 */
	let { name, logoUrl, class: className }: { name: string; logoUrl?: string | null; class?: string } = $props();

	const src = $derived(logoUrl === undefined ? storeSettings(page.data.config?.settings).logoUrl : logoUrl);
</script>

<span class={cn('inline-flex min-w-0 items-center gap-2 font-semibold tracking-tight', className)}>
	{#if src}
		<img {src} alt={name} class="h-8 w-auto max-w-40 object-contain" />
	{:else}
		<span class="grid size-8 shrink-0 place-items-center rounded-lg bg-primary text-primary-foreground">
			<Rocket class="size-4" />
		</span>
		<span class="truncate">{name}</span>
	{/if}
</span>
