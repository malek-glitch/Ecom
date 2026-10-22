<script lang="ts">
	import { page } from '$app/state';
	import Seo from '$lib/components/app/Seo.svelte';
	import CatalogView from '$lib/components/shop/CatalogView.svelte';
	import * as Breadcrumb from '$lib/components/ui/breadcrumb';
	import { m } from '$lib/paraglide/messages';
	import { breadcrumbs } from '$lib/seo/json-ld';
	import { isFiltered } from '$lib/shop/catalog';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const currency = $derived(shopSettings(data.config.settings).currency);
	const category = $derived(data.category);
	const path = $derived(`/categories/${category.slug}`);
	const description = $derived(
		category.seoDescription ||
			category.description ||
			m.category_meta_description({ name: category.name, app: data.config.appName }),
	);
</script>

<Seo
	title={category.seoTitle || category.name}
	{description}
	canonical={data.filters.page ? `${path}?page=${data.filters.page}` : path}
	noindex={isFiltered(data.filters)}
	jsonLd={breadcrumbs(page.url.origin, [
		{ name: m.nav_home(), path: '/' },
		{ name: m.shop_all_products(), path: '/products' },
		{ name: category.name, path },
	])}
/>

<CatalogView results={data.results} filters={data.filters} {currency} category={category.slug}>
	{#snippet header()}
		<Breadcrumb.Root class="mb-4">
			<Breadcrumb.List>
				<Breadcrumb.Item><Breadcrumb.Link href="/">{m.nav_home()}</Breadcrumb.Link></Breadcrumb.Item>
				<Breadcrumb.Separator />
				<Breadcrumb.Item><Breadcrumb.Link href="/products">{m.shop_all_products()}</Breadcrumb.Link></Breadcrumb.Item>
				<Breadcrumb.Separator />
				<Breadcrumb.Item><Breadcrumb.Page>{category.name}</Breadcrumb.Page></Breadcrumb.Item>
			</Breadcrumb.List>
		</Breadcrumb.Root>
		<div class="mb-6 space-y-2">
			<h1 class="text-3xl font-semibold tracking-tight text-balance">
				{data.filters.q ? m.search_results_in({ query: data.filters.q, category: category.name }) : category.name}
			</h1>
			{#if category.description}
				<p class="max-w-3xl text-muted-foreground" dir="auto">{category.description}</p>
			{/if}
		</div>
	{/snippet}
</CatalogView>
