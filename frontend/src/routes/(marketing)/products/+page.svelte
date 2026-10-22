<script lang="ts">
	import X from '@lucide/svelte/icons/x';
	import Seo from '$lib/components/app/Seo.svelte';
	import CatalogView from '$lib/components/shop/CatalogView.svelte';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';
	import { isFiltered } from '$lib/shop/catalog';
	import { absoluteUrl } from '$lib/seo/json-ld';
	import { page } from '$app/state';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const currency = $derived(shopSettings(data.config.settings).currency);
	const title = $derived(data.filters.q ? m.search_results_title({ query: data.filters.q }) : m.shop_all_products());
	const itemList = $derived({
		'@context': 'https://schema.org',
		'@type': 'ItemList',
		itemListElement: data.results.items.map((product, index) => ({
			'@type': 'ListItem',
			position: data.results.page * data.results.size + index + 1,
			url: absoluteUrl(page.url.origin, `/products/${product.slug}`),
			name: product.name,
		})),
	});
</script>

<!-- Search results and filtered lists stay out of search engines; the plain catalog is indexed, page by page. -->
<Seo
	{title}
	description={m.shop_catalog_description({ app: data.config.appName })}
	canonical={data.filters.page ? `/products?page=${data.filters.page}` : '/products'}
	noindex={isFiltered(data.filters)}
	jsonLd={itemList}
/>

<CatalogView results={data.results} filters={data.filters} {currency}>
	{#snippet header()}
		<div class="mb-6 flex flex-wrap items-end justify-between gap-3">
			<div class="space-y-1">
				<h1 class="text-3xl font-semibold tracking-tight text-balance">{title}</h1>
				{#if !data.filters.q}<p class="text-muted-foreground">
						{m.shop_catalog_description({ app: data.config.appName })}
					</p>{/if}
			</div>
			{#if data.filters.q}
				<Button variant="ghost" size="sm" href="/products"><X />{m.search_clear()}</Button>
			{/if}
		</div>
	{/snippet}
</CatalogView>
