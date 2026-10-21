<script lang="ts">
	import Package from '@lucide/svelte/icons/package';
	import { cn } from '$lib/utils';

	/** The product photo, or a neutral placeholder when there is none (storage off, no upload yet). */
	let { src, alt, class: className = '' }: { src?: string | null; alt: string; class?: string } = $props();
</script>

<div class={cn('relative overflow-hidden bg-muted', className)}>
	{#if src}
		<img {src} {alt} loading="lazy" decoding="async" class="size-full object-cover" />
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
