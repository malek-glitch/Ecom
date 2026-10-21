<script lang="ts">
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { cn } from '$lib/utils';

	let {
		priceCents,
		compareAtPriceCents = null,
		currency,
		class: className = '',
	}: { priceCents: number; compareAtPriceCents?: number | null; currency: string; class?: string } = $props();

	const onSale = $derived(compareAtPriceCents != null && compareAtPriceCents > priceCents);
</script>

<span class={cn('inline-flex flex-wrap items-baseline gap-x-2 tabular-nums', className)}>
	<span class={cn('font-semibold', onSale && 'text-destructive')}>{formatMoney(priceCents, currency)}</span>
	{#if onSale}
		<span class="text-[0.85em] text-muted-foreground line-through">
			<span class="sr-only">{m.shop_was_price()}</span>
			{formatMoney(compareAtPriceCents!, currency)}
		</span>
	{/if}
</span>
