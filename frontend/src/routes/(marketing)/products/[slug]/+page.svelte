<script lang="ts">
	import ShoppingBag from '@lucide/svelte/icons/shopping-bag';
	import { toast } from 'svelte-sonner';
	import { goto } from '$app/navigation';
	import Seo from '$lib/components/app/Seo.svelte';
	import Price from '$lib/components/shop/Price.svelte';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import QuantityInput from '$lib/components/shop/QuantityInput.svelte';
	import StockBadge from '$lib/components/shop/StockBadge.svelte';
	import * as Breadcrumb from '$lib/components/ui/breadcrumb';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { m } from '$lib/paraglide/messages';
	import { cart, MAX_LINE_QUANTITY } from '$lib/shop/cart.svelte';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const product = $derived(data.product);
	const shop = $derived(shopSettings(data.config.settings));
	const inCart = $derived(cart.quantityOf(product.id));
	/** What can still be added, given the stock and what is already in the cart. */
	const available = $derived(Math.max(0, Math.min(product.stock, MAX_LINE_QUANTITY) - inCart));

	let quantity = $state(1);

	function addToCart() {
		const added = cart.add(
			{
				productId: product.id,
				slug: product.slug,
				name: product.name,
				priceCents: product.priceCents,
				imageUrl: product.imageUrl,
			},
			Math.min(quantity, available),
		);
		if (!added) {
			toast.error(m.cart_full());
			return;
		}
		quantity = 1;
		toast.success(m.cart_added({ name: product.name }), {
			action: { label: m.cart_view(), onClick: () => void goto('/cart') },
		});
	}
</script>

<Seo title={product.name} description={product.description.slice(0, 160) || undefined} />

<div class="mx-auto max-w-6xl px-4 py-8 sm:px-6">
	<Breadcrumb.Root class="mb-6">
		<Breadcrumb.List>
			<Breadcrumb.Item><Breadcrumb.Link href="/products">{m.shop_all_products()}</Breadcrumb.Link></Breadcrumb.Item>
			{#if product.category}
				<Breadcrumb.Separator />
				<Breadcrumb.Item>
					<Breadcrumb.Link href="/products?category={product.category.slug}">{product.category.name}</Breadcrumb.Link>
				</Breadcrumb.Item>
			{/if}
			<Breadcrumb.Separator />
			<Breadcrumb.Item><Breadcrumb.Page class="line-clamp-1">{product.name}</Breadcrumb.Page></Breadcrumb.Item>
		</Breadcrumb.List>
	</Breadcrumb.Root>

	<div class="grid gap-8 md:grid-cols-2 lg:gap-12">
		<ProductImage src={product.imageUrl} alt={product.name} class="aspect-square rounded-2xl border" />

		<div class="flex flex-col gap-6">
			<div class="space-y-3">
				<h1 class="text-3xl font-semibold tracking-tight text-balance">{product.name}</h1>
				<Price
					priceCents={product.priceCents}
					compareAtPriceCents={product.compareAtPriceCents}
					currency={shop.currency}
					class="text-2xl"
				/>
				<div><StockBadge stock={product.stock} showInStock /></div>
			</div>

			{#if product.stock > 0}
				<div class="flex flex-wrap items-center gap-3">
					<QuantityInput
						bind:value={quantity}
						max={Math.max(1, available)}
						label={m.cart_quantity()}
						disabled={available === 0}
					/>
					<Button size="lg" class="flex-1 sm:flex-none" disabled={available === 0} onclick={addToCart}>
						<ShoppingBag />{m.cart_add()}
					</Button>
				</div>
				{#if inCart > 0}
					<p class="text-sm text-muted-foreground">
						{m.cart_already({ count: inCart })}
						<a href="/cart" class="font-medium text-foreground underline-offset-4 hover:underline">{m.cart_view()}</a>
					</p>
				{/if}
			{:else}
				<Button size="lg" disabled>{m.shop_out_of_stock()}</Button>
			{/if}

			{#if product.description}
				<Separator />
				<div class="space-y-2">
					<h2 class="font-medium">{m.shop_description()}</h2>
					<p class="leading-relaxed whitespace-pre-line text-muted-foreground">{product.description}</p>
				</div>
			{/if}
			{#if product.sku}
				<p class="text-xs text-muted-foreground">{m.shop_sku()}: <span dir="ltr">{product.sku}</span></p>
			{/if}
		</div>
	</div>
</div>
