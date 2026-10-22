<script lang="ts">
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import ShoppingBag from '@lucide/svelte/icons/shopping-bag';
	import Truck from '@lucide/svelte/icons/truck';
	import { toast } from 'svelte-sonner';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import Seo from '$lib/components/app/Seo.svelte';
	import Price from '$lib/components/shop/Price.svelte';
	import ProductGrid from '$lib/components/shop/ProductGrid.svelte';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import QuantityInput from '$lib/components/shop/QuantityInput.svelte';
	import StockBadge from '$lib/components/shop/StockBadge.svelte';
	import * as Breadcrumb from '$lib/components/ui/breadcrumb';
	import { Button } from '$lib/components/ui/button';
	import { Separator } from '$lib/components/ui/separator';
	import { currencyDigits, formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { absoluteUrl, breadcrumbs, decimalPrice } from '$lib/seo/json-ld';
	import { cart, MAX_LINE_QUANTITY } from '$lib/shop/cart.svelte';
	import { orderableUnits, shopSettings } from '$lib/shop/store-api';
	import { IMAGE_RATIO_CLASS, storeSettings } from '$lib/theme/store-settings';
	import { cn } from '$lib/utils';

	let { data } = $props();

	const product = $derived(data.product);
	const shop = $derived(shopSettings(data.config.settings));
	const ratio = $derived(IMAGE_RATIO_CLASS[storeSettings(data.config.settings).productImageRatio]);
	const inCart = $derived(cart.quantityOf(product.id));
	/** What can still be added, given the stock and what is already in the cart. */
	const available = $derived(Math.max(0, orderableUnits(product.stock, MAX_LINE_QUANTITY) - inCart));
	/** Null stock: not tracked, always available. */
	const inStock = $derived(product.stock == null || product.stock > 0);

	let quantity = $state(1);

	const description = $derived(
		product.seoDescription || product.description.replace(/\s+/g, ' ').slice(0, 160) || undefined,
	);
	const origin = $derived(page.url.origin);
	const jsonLd = $derived([
		{
			'@context': 'https://schema.org',
			'@type': 'Product',
			name: product.name,
			description: product.description || undefined,
			sku: product.sku ?? undefined,
			image: absoluteUrl(origin, product.imageUrl),
			category: product.category?.name,
			url: `${origin}/products/${product.slug}`,
			offers: {
				'@type': 'Offer',
				price: decimalPrice(product.priceCents, currencyDigits(shop.currency)),
				priceCurrency: shop.currency,
				availability: inStock ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock',
				itemCondition: 'https://schema.org/NewCondition',
				url: `${origin}/products/${product.slug}`,
				seller: { '@type': 'Organization', name: data.config.appName },
			},
		},
		breadcrumbs(origin, [
			{ name: m.nav_home(), path: '/' },
			...(product.category
				? [{ name: product.category.name, path: `/categories/${product.category.slug}` }]
				: [{ name: m.shop_all_products(), path: '/products' }]),
			{ name: product.name, path: `/products/${product.slug}` },
		]),
	]);

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

<Seo
	title={product.seoTitle || product.name}
	{description}
	image={product.imageUrl}
	type="product"
	canonical="/products/{product.slug}"
	{jsonLd}
/>

<div class="mx-auto max-w-7xl px-4 py-8 pb-28 sm:px-6 md:pb-8">
	<Breadcrumb.Root class="mb-6">
		<Breadcrumb.List>
			<Breadcrumb.Item><Breadcrumb.Link href="/">{m.nav_home()}</Breadcrumb.Link></Breadcrumb.Item>
			<Breadcrumb.Separator />
			{#if product.category}
				<Breadcrumb.Item>
					<Breadcrumb.Link href="/categories/{product.category.slug}">{product.category.name}</Breadcrumb.Link>
				</Breadcrumb.Item>
			{:else}
				<Breadcrumb.Item><Breadcrumb.Link href="/products">{m.shop_all_products()}</Breadcrumb.Link></Breadcrumb.Item>
			{/if}
			<Breadcrumb.Separator />
			<Breadcrumb.Item><Breadcrumb.Page class="line-clamp-1">{product.name}</Breadcrumb.Page></Breadcrumb.Item>
		</Breadcrumb.List>
	</Breadcrumb.Root>

	<div class="grid gap-8 md:grid-cols-2 lg:gap-14">
		<ProductImage src={product.imageUrl} alt={product.name} eager class={cn('w-full rounded-2xl border', ratio)} />

		<div class="flex flex-col gap-6">
			<div class="space-y-3">
				{#if product.category}
					<a href="/categories/{product.category.slug}" class="text-sm text-muted-foreground hover:text-foreground">
						{product.category.name}
					</a>
				{/if}
				<h1 class="text-3xl font-semibold tracking-tight text-balance sm:text-4xl">{product.name}</h1>
				<Price
					priceCents={product.priceCents}
					compareAtPriceCents={product.compareAtPriceCents}
					currency={shop.currency}
					class="text-2xl"
				/>
				<div><StockBadge stock={product.stock} lowStock={product.lowStock} showInStock /></div>
			</div>

			{#if inStock}
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

			<ul class="grid gap-3 rounded-xl border bg-muted/30 p-4 text-sm sm:grid-cols-3 md:grid-cols-1 xl:grid-cols-3">
				<li class="flex items-center gap-2">
					<Truck class="size-4 shrink-0 text-primary" />
					{shop.freeShippingThresholdCents > 0
						? m.product_perk_shipping({ amount: formatMoney(shop.freeShippingThresholdCents, shop.currency) })
						: m.shop_perk_shipping_title()}
				</li>
				<li class="flex items-center gap-2">
					<ShieldCheck class="size-4 shrink-0 text-primary" />{m.shop_perk_secure_title()}
				</li>
				<li class="flex items-center gap-2">
					<RotateCcw class="size-4 shrink-0 text-primary" />{m.shop_perk_returns_title()}
				</li>
			</ul>

			{#if product.description}
				<Separator />
				<div class="space-y-2">
					<h2 class="font-medium">{m.shop_description()}</h2>
					<p class="leading-relaxed whitespace-pre-line text-muted-foreground" dir="auto">{product.description}</p>
				</div>
			{/if}
			{#if product.sku}
				<p class="text-xs text-muted-foreground">{m.shop_sku()}: <span dir="ltr">{product.sku}</span></p>
			{/if}
		</div>
	</div>

	{#if product.related.length}
		<section class="mt-16" aria-labelledby="related-title">
			<h2 id="related-title" class="mb-6 text-2xl font-semibold tracking-tight">{m.product_related()}</h2>
			<ProductGrid products={product.related} currency={shop.currency} />
		</section>
	{/if}
</div>

<!-- Phones: the price and the button stay in reach while reading. -->
{#if inStock}
	<div
		class="fixed inset-x-0 bottom-0 z-20 flex items-center gap-3 border-t bg-background/95 px-4 py-3 backdrop-blur md:hidden"
	>
		<div class="min-w-0 flex-1">
			<p class="truncate text-sm font-medium">{product.name}</p>
			<Price
				priceCents={product.priceCents}
				compareAtPriceCents={product.compareAtPriceCents}
				currency={shop.currency}
				class="text-sm"
			/>
		</div>
		<Button disabled={available === 0} onclick={addToCart}><ShoppingBag />{m.cart_add()}</Button>
	</div>
{/if}
