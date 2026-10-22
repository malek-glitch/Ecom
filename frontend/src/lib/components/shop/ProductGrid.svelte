<script lang="ts">
	import type { ProductSummary } from '$lib/shop/store-api';
	import { cn } from '$lib/utils';
	import ProductCard from './ProductCard.svelte';

	let {
		products,
		currency,
		query = '',
		class: className = '',
	}: { products: ProductSummary[]; currency: string; query?: string; class?: string } = $props();
</script>

<div class={cn('grid grid-cols-2 gap-x-4 gap-y-8 md:grid-cols-3 lg:grid-cols-4', className)}>
	{#each products as product (product.id)}
		<ProductCard {product} {currency} {query} />
	{/each}
</div>
