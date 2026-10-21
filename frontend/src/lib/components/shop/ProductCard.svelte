<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import { m } from '$lib/paraglide/messages';
	import type { ProductSummary } from '$lib/shop/store-api';
	import Price from './Price.svelte';
	import ProductImage from './ProductImage.svelte';
	import StockBadge from './StockBadge.svelte';

	let { product, currency }: { product: ProductSummary; currency: string } = $props();

	const onSale = $derived(product.compareAtPriceCents != null && product.compareAtPriceCents > product.priceCents);
</script>

<a
	href="/products/{product.slug}"
	class="group flex flex-col gap-3 rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
>
	<div class="relative">
		<ProductImage
			src={product.imageUrl}
			alt={product.name}
			class="aspect-square rounded-xl border transition-colors group-hover:border-primary/40"
		/>
		<div class="absolute start-2 top-2 flex flex-wrap gap-1">
			{#if onSale}
				<Badge>{m.shop_sale()}</Badge>
			{/if}
			<StockBadge stock={product.stock} />
		</div>
	</div>
	<div class="space-y-1 px-0.5">
		{#if product.category}
			<p class="text-xs text-muted-foreground">{product.category.name}</p>
		{/if}
		<h3 class="line-clamp-2 leading-snug font-medium group-hover:underline">{product.name}</h3>
		<Price priceCents={product.priceCents} compareAtPriceCents={product.compareAtPriceCents} {currency} />
	</div>
</a>
