<script lang="ts">
	import type { ProductSummary } from '$lib/shop/store-api';
	import ProductCard from './ProductCard.svelte';

	let { products, currency }: { products: ProductSummary[]; currency: string } = $props();
</script>

<div class="grid grid-cols-2 gap-x-4 gap-y-8 md:grid-cols-3 lg:grid-cols-4">
	{#each products as product (product.id)}
		<ProductCard {product} {currency} />
	{/each}
</div>
