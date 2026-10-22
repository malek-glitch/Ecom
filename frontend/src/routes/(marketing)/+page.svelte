<script lang="ts">
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import Truck from '@lucide/svelte/icons/truck';
	import { page } from '$app/state';
	import Seo from '$lib/components/app/Seo.svelte';
	import ProductGrid from '$lib/components/shop/ProductGrid.svelte';
	import { Button } from '$lib/components/ui/button';
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { absoluteUrl } from '$lib/seo/json-ld';
	import { shopSettings } from '$lib/shop/store-api';
	import { storeSettings } from '$lib/theme/store-settings';
	import { cn } from '$lib/utils';

	let { data } = $props();

	// Texts and picture come from Administration → Appearance; empty ones use the translated defaults.
	const shop = $derived(shopSettings(data.config.settings));
	const store = $derived(storeSettings(data.config.settings));
	const kicker = $derived(store.heroKicker || m.shop_hero_kicker());
	const title = $derived(store.heroTitle || m.shop_hero_title());
	const subtitle = $derived(store.heroSubtitle || m.shop_hero_subtitle({ app: data.config.appName }));
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

	// Lets search engines show the store's name, logo and a search box in their results.
	const origin = $derived(page.url.origin);
	const jsonLd = $derived([
		{
			'@context': 'https://schema.org',
			'@type': 'Organization',
			name: data.config.appName,
			url: `${origin}/`,
			logo: absoluteUrl(origin, store.logoUrl),
			sameAs: Object.values(store.social).filter(Boolean),
		},
		{
			'@context': 'https://schema.org',
			'@type': 'WebSite',
			name: data.config.appName,
			url: `${origin}/`,
			potentialAction: {
				'@type': 'SearchAction',
				target: { '@type': 'EntryPoint', urlTemplate: `${origin}/products?q={search_term_string}` },
				'query-input': 'required name=search_term_string',
			},
		},
	]);
</script>

<Seo
	description={store.metaDescription || subtitle}
	image={store.shareImageUrl ?? store.heroImageUrl}
	canonical="/"
	{jsonLd}
/>

<section class="relative overflow-hidden border-b">
	<div
		aria-hidden="true"
		class="pointer-events-none absolute inset-x-0 -top-40 -z-10 h-[36rem] bg-gradient-to-b from-primary/15 via-primary/5 to-transparent blur-2xl"
	></div>
	<div
		class={cn(
			'mx-auto grid max-w-7xl items-center gap-10 px-4 py-16 sm:px-6 sm:py-24',
			store.heroImageUrl ? 'lg:grid-cols-2' : 'max-w-3xl text-center',
		)}
	>
		<div>
			<p class="mb-4 text-sm font-medium tracking-wide text-primary" dir="auto">{kicker}</p>
			<h1 class="text-4xl font-semibold tracking-tight text-balance sm:text-5xl lg:text-6xl" dir="auto">{title}</h1>
			<p
				class="mt-6 max-w-2xl text-lg text-pretty text-muted-foreground {store.heroImageUrl ? '' : 'mx-auto'}"
				dir="auto"
			>
				{subtitle}
			</p>
			<div class={cn('mt-10 flex flex-wrap items-center gap-3', !store.heroImageUrl && 'justify-center')}>
				<Button size="lg" href="/products">{m.shop_hero_cta()}<ArrowRight class="rtl:rotate-180" /></Button>
				{#if categories[0]}
					<Button size="lg" variant="outline" href="/categories/{categories[0].slug}">{categories[0].name}</Button>
				{/if}
			</div>
		</div>
		{#if store.heroImageUrl}
			<img
				src={store.heroImageUrl}
				alt=""
				fetchpriority="high"
				width="1200"
				height="900"
				class="aspect-[4/3] w-full rounded-2xl border object-cover shadow-xl"
			/>
		{/if}
	</div>
</section>

{#if categories.length}
	<section class="mx-auto max-w-7xl px-4 pt-14 sm:px-6" aria-labelledby="categories-title">
		<h2 id="categories-title" class="mb-6 text-2xl font-semibold tracking-tight">{m.shop_shop_by_category()}</h2>
		<ul class="grid grid-cols-2 gap-3 md:grid-cols-4">
			{#each categories.slice(0, 8) as category, index (category.id)}
				<li>
					<a
						href="/categories/{category.slug}"
						class="group flex h-full flex-col justify-between gap-6 rounded-xl border p-5 transition-colors outline-none hover:border-primary/50 hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring/50 {index ===
						0
							? 'bg-primary/5'
							: 'bg-card'}"
					>
						<span class="text-lg font-medium" dir="auto">{category.name}</span>
						<span class="flex items-center justify-between text-sm text-muted-foreground">
							{m.products_count({ count: category.productCount })}
							<ArrowRight
								class="size-4 transition-transform group-hover:translate-x-0.5 rtl:rotate-180 rtl:group-hover:-translate-x-0.5"
							/>
						</span>
					</a>
				</li>
			{/each}
		</ul>
	</section>
{/if}

{#if data.home.featured.length}
	<section class="mx-auto max-w-7xl px-4 pt-14 sm:px-6" aria-labelledby="featured-title">
		<h2 id="featured-title" class="mb-6 text-2xl font-semibold tracking-tight">{m.shop_featured()}</h2>
		<ProductGrid products={data.home.featured} currency={shop.currency} />
	</section>
{/if}

<section class="mx-auto max-w-7xl px-4 pt-14 sm:px-6" aria-labelledby="new-title">
	<div class="mb-6 flex flex-wrap items-end justify-between gap-2">
		<h2 id="new-title" class="text-2xl font-semibold tracking-tight">{m.shop_new_arrivals()}</h2>
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

{#if store.showPerks}
	<section class="mx-auto max-w-7xl px-4 pt-16 sm:px-6">
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
{/if}
