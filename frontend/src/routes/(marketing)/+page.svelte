<script lang="ts">
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import Truck from '@lucide/svelte/icons/truck';
	import Seo from '$lib/components/app/Seo.svelte';
	import ProductGrid from '$lib/components/shop/ProductGrid.svelte';
	import { Button } from '$lib/components/ui/button';
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	// The store's home page. Replace the copy (messages shop_hero_*) and the perks with your own.
	const shop = $derived(shopSettings(data.config.settings));
	const categories = $derived(data.home.categories.filter((category) => category.productCount > 0));
	const perks = $derived([
		{
			icon: Truck,
			title: m.shop_perk_shipping_title(),
			body:
				shop.freeShippingThresholdCents > 0
					? m.shop_perk_shipping_body({ amount: formatMoney(shop.freeShippingThresholdCents, shop.currency) })
					: m.shop_perk_shipping_body_flat(),
		},
		{ icon: ShieldCheck, title: m.shop_perk_secure_title(), body: m.shop_perk_secure_body() },
		{ icon: RotateCcw, title: m.shop_perk_returns_title(), body: m.shop_perk_returns_body() },
	]);
</script>

<Seo description={m.shop_hero_subtitle({ app: data.config.appName })} />

<section class="relative overflow-hidden border-b">
	<div
		aria-hidden="true"
		class="pointer-events-none absolute inset-x-0 -top-40 -z-10 h-[32rem] bg-gradient-to-b from-primary/10 to-transparent blur-2xl"
	></div>
	<div class="mx-auto max-w-3xl px-4 py-16 text-center sm:px-6 sm:py-24">
		<p class="mb-4 text-sm font-medium tracking-wide text-muted-foreground">{m.shop_hero_kicker()}</p>
		<h1 class="text-4xl font-semibold tracking-tight text-balance sm:text-5xl">{m.shop_hero_title()}</h1>
		<p class="mx-auto mt-6 max-w-2xl text-lg text-pretty text-muted-foreground">
			{m.shop_hero_subtitle({ app: data.config.appName })}
		</p>
		<div class="mt-10 flex flex-wrap items-center justify-center gap-3">
			<Button size="lg" href="/products">{m.shop_hero_cta()}<ArrowRight class="rtl:rotate-180" /></Button>
		</div>
	</div>
</section>

{#if categories.length}
	<section class="mx-auto max-w-6xl px-4 pt-12 sm:px-6">
		<h2 class="sr-only">{m.shop_categories()}</h2>
		<ul class="flex flex-wrap justify-center gap-2">
			{#each categories as category (category.id)}
				<li>
					<Button variant="outline" size="sm" href="/products?category={category.slug}" class="rounded-full">
						{category.name}
					</Button>
				</li>
			{/each}
		</ul>
	</section>
{/if}

{#if data.home.featured.length}
	<section class="mx-auto max-w-6xl px-4 pt-12 sm:px-6">
		<h2 class="mb-6 text-2xl font-semibold tracking-tight">{m.shop_featured()}</h2>
		<ProductGrid products={data.home.featured} currency={shop.currency} />
	</section>
{/if}

<section class="mx-auto max-w-6xl px-4 pt-16 sm:px-6">
	<div class="mb-6 flex flex-wrap items-end justify-between gap-2">
		<h2 class="text-2xl font-semibold tracking-tight">{m.shop_new_arrivals()}</h2>
		<Button variant="link" href="/products" class="px-0">
			{m.shop_view_all()}<ArrowRight class="rtl:rotate-180" />
		</Button>
	</div>
	{#if data.home.newest.length}
		<ProductGrid products={data.home.newest} currency={shop.currency} />
	{:else}
		<p class="rounded-xl border border-dashed p-10 text-center text-muted-foreground">{m.shop_empty_catalog()}</p>
	{/if}
</section>

<section class="mx-auto max-w-6xl px-4 py-16 sm:px-6">
	<ul class="grid gap-6 rounded-2xl border bg-muted/30 p-6 sm:grid-cols-3 sm:p-8">
		{#each perks as perk (perk.title)}
			<li class="flex gap-4">
				<span class="grid size-10 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
					<perk.icon class="size-5" />
				</span>
				<div>
					<h3 class="font-medium">{perk.title}</h3>
					<p class="mt-1 text-sm text-pretty text-muted-foreground">{perk.body}</p>
				</div>
			</li>
		{/each}
	</ul>
</section>
