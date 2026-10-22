<script lang="ts">
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import ShoppingBag from '@lucide/svelte/icons/shopping-bag';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import OrderTotals from '$lib/components/shop/OrderTotals.svelte';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import QuantityInput from '$lib/components/shop/QuantityInput.svelte';
	import * as Alert from '$lib/components/ui/alert';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Empty from '$lib/components/ui/empty';
	import { Skeleton } from '$lib/components/ui/skeleton';
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { cart, MAX_LINE_QUANTITY } from '$lib/shop/cart.svelte';
	import { createCartQuote } from '$lib/shop/quote.svelte';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const quote = createCartQuote();
	const currency = $derived(quote.quote?.currency ?? shopSettings(data.config.settings).currency);
	const shortLines = $derived(quote.quote?.lines.filter((line) => line.quantity > line.stock) ?? []);
	const missingForFreeShipping = $derived.by(() => {
		const q = quote.quote;
		if (!q?.freeShippingThresholdCents || q.shippingCents === 0) return 0;
		return Math.max(0, q.freeShippingThresholdCents - q.subtotalCents);
	});
</script>

<Seo title={m.cart_title()} noindex />

<div class="mx-auto max-w-6xl px-4 py-10 sm:px-6">
	<h1 class="mb-8 text-3xl font-semibold tracking-tight">{m.cart_title()}</h1>

	{#if cart.isEmpty}
		<Empty.Root class="border border-dashed">
			<Empty.Header>
				<Empty.Media variant="icon"><ShoppingBag /></Empty.Media>
				<Empty.Title>{m.cart_empty_title()}</Empty.Title>
				<Empty.Description>{m.cart_empty_body()}</Empty.Description>
			</Empty.Header>
			<Empty.Content>
				<Button href="/products">{m.shop_hero_cta()}</Button>
			</Empty.Content>
		</Empty.Root>
	{:else}
		<div class="grid gap-8 lg:grid-cols-[1fr_22rem]">
			<section aria-label={m.cart_title()} class="space-y-4">
				{#if shortLines.length}
					<Alert.Root variant="destructive">
						<Alert.Description>{m.cart_stock_problem()}</Alert.Description>
					</Alert.Root>
				{/if}
				<ul class="divide-y rounded-xl border">
					{#each cart.items as item (item.productId)}
						{@const line = quote.line(item.productId)}
						<li class="flex gap-4 p-4">
							<a href="/products/{item.slug}" class="shrink-0" tabindex="-1" aria-hidden="true">
								<ProductImage src={item.imageUrl} alt="" class="size-20 rounded-lg border sm:size-24" />
							</a>
							<div class="flex min-w-0 flex-1 flex-col gap-2">
								<div class="flex items-start justify-between gap-3">
									<a href="/products/{item.slug}" class="line-clamp-2 font-medium hover:underline">{item.name}</a>
									<span class="shrink-0 font-semibold tabular-nums">
										{formatMoney(line?.lineTotalCents ?? item.priceCents * item.quantity, currency)}
									</span>
								</div>
								<p class="text-sm text-muted-foreground tabular-nums">
									{m.cart_each({ price: formatMoney(line?.unitPriceCents ?? item.priceCents, currency) })}
								</p>
								{#if line && line.quantity > line.stock}
									<p class="text-sm text-destructive">
										{line.stock === 0 ? m.shop_out_of_stock() : m.shop_low_stock({ count: line.stock })}
									</p>
								{/if}
								<div class="mt-auto flex items-center justify-between gap-2">
									<QuantityInput
										value={item.quantity}
										max={MAX_LINE_QUANTITY}
										label={m.cart_quantity_of({ name: item.name })}
										onchange={(value) => cart.setQuantity(item.productId, value)}
									/>
									<Button
										variant="ghost"
										size="sm"
										class="text-muted-foreground"
										onclick={() => cart.remove(item.productId)}
									>
										<Trash2 />{m.cart_remove()}
									</Button>
								</div>
							</div>
						</li>
					{/each}
				</ul>
				<Button variant="link" href="/products" class="px-0">{m.cart_continue()}</Button>
			</section>

			<aside>
				<Card.Root class="lg:sticky lg:top-24">
					<Card.Header>
						<Card.Title>{m.cart_summary()}</Card.Title>
					</Card.Header>
					<Card.Content class="space-y-4">
						<FormAlert message={quote.error} />
						{#if quote.quote}
							<OrderTotals
								subtotalCents={quote.quote.subtotalCents}
								shippingCents={quote.quote.shippingCents}
								totalCents={quote.quote.totalCents}
								{currency}
							/>
							{#if missingForFreeShipping > 0}
								<p class="rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">
									{m.cart_free_shipping_hint({ amount: formatMoney(missingForFreeShipping, currency) })}
								</p>
							{/if}
						{:else}
							<div class="space-y-2">
								<Skeleton class="h-5 w-full" />
								<Skeleton class="h-5 w-full" />
								<Skeleton class="h-6 w-full" />
							</div>
						{/if}
					</Card.Content>
					<Card.Footer>
						<Button size="lg" class="w-full" href="/checkout" disabled={!quote.quote?.orderable || quote.loading}>
							{m.cart_checkout()}<ArrowRight class="rtl:rotate-180" />
						</Button>
					</Card.Footer>
				</Card.Root>
			</aside>
		</div>
	{/if}
</div>
