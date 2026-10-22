<script lang="ts">
	import Plus from '@lucide/svelte/icons/plus';
	import { toast } from 'svelte-sonner';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { Badge } from '$lib/components/ui/badge';
	import { m } from '$lib/paraglide/messages';
	import { cart } from '$lib/shop/cart.svelte';
	import type { ProductSummary } from '$lib/shop/store-api';
	import { IMAGE_RATIO_CLASS, storeSettings } from '$lib/theme/store-settings';
	import { cn } from '$lib/utils';
	import Highlight from './Highlight.svelte';
	import Price from './Price.svelte';
	import ProductImage from './ProductImage.svelte';
	import StockBadge from './StockBadge.svelte';

	/** [query] marks the matching words of the name (search results). */
	let { product, currency, query = '' }: { product: ProductSummary; currency: string; query?: string } = $props();

	const onSale = $derived(product.compareAtPriceCents != null && product.compareAtPriceCents > product.priceCents);
	const ratio = $derived(IMAGE_RATIO_CLASS[storeSettings(page.data.config?.settings).productImageRatio]);
	const canAdd = $derived(product.stock > cart.quantityOf(product.id));

	function quickAdd() {
		const added = cart.add({
			productId: product.id,
			slug: product.slug,
			name: product.name,
			priceCents: product.priceCents,
			imageUrl: product.imageUrl,
		});
		if (!added) return void toast.error(m.cart_full());
		toast.success(m.cart_added({ name: product.name }), {
			action: { label: m.cart_view(), onClick: () => void goto('/cart') },
		});
	}
</script>

<article class="group relative flex flex-col gap-3">
	<div class="relative overflow-hidden rounded-xl border transition-shadow group-hover:shadow-md">
		<ProductImage
			src={product.imageUrl}
			alt={product.name}
			class={cn('w-full transition-transform duration-500 group-hover:scale-[1.03]', ratio)}
		/>
		<div class="pointer-events-none absolute start-2 top-2 flex flex-wrap gap-1">
			{#if onSale}
				<Badge>{m.shop_sale()}</Badge>
			{/if}
			<StockBadge stock={product.stock} />
		</div>
		{#if product.stock > 0}
			<button
				type="button"
				class="absolute end-2 bottom-2 z-10 grid size-9 place-items-center rounded-full bg-background/95 text-foreground shadow-md ring-1 ring-border transition hover:bg-primary hover:text-primary-foreground focus-visible:opacity-100 focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-40 md:translate-y-1 md:opacity-0 md:group-hover:translate-y-0 md:group-hover:opacity-100"
				aria-label={m.cart_add_named({ name: product.name })}
				disabled={!canAdd}
				onclick={quickAdd}
			>
				<Plus class="size-4" />
			</button>
		{/if}
	</div>
	<div class="space-y-1 px-0.5">
		{#if product.category}
			<p class="text-xs text-muted-foreground">{product.category.name}</p>
		{/if}
		<h3 class="line-clamp-2 leading-snug font-medium">
			<!-- The link's ::after covers the whole card, so anywhere but the + button opens the product. -->
			<a
				href="/products/{product.slug}"
				class="outline-none group-hover:underline after:absolute after:inset-0 after:rounded-xl focus-visible:after:ring-[3px] focus-visible:after:ring-ring/50"
			>
				<Highlight text={product.name} {query} />
			</a>
		</h3>
		<Price priceCents={product.priceCents} compareAtPriceCents={product.compareAtPriceCents} {currency} />
	</div>
</article>
