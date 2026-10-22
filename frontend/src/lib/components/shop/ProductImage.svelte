<script lang="ts">
	import Package from '@lucide/svelte/icons/package';
	import { cn } from '$lib/utils';

	/** The product photo, or a neutral placeholder when there is none (storage off, no upload yet). */
	/** [eager]: the main picture of a page, loaded at once (better Largest Contentful Paint). */
	let {
		src,
		alt,
		eager = false,
		class: className = '',
	}: { src?: string | null; alt: string; eager?: boolean; class?: string } = $props();
</script>

<div class={cn('relative overflow-hidden bg-muted', className)}>
	{#if src}
		<img
			{src}
			{alt}
			loading={eager ? 'eager' : 'lazy'}
			fetchpriority={eager ? 'high' : undefined}
			decoding="async"
			width="1200"
			height="1200"
			class="size-full object-cover"
		/>
	{:else}
		<div
			class="grid size-full place-items-center bg-gradient-to-br from-muted to-primary/10 text-muted-foreground/60"
			role="img"
			aria-label={alt}
		>
			<Package class="size-1/4 min-h-6 min-w-6" strokeWidth={1.25} />
		</div>
	{/if}
</div>
