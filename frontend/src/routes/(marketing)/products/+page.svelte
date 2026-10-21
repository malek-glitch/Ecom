<script lang="ts">
	import PackageSearch from '@lucide/svelte/icons/package-search';
	import Search from '@lucide/svelte/icons/search';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import ProductGrid from '$lib/components/shop/ProductGrid.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Empty from '$lib/components/ui/empty';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import { debounce } from '$lib/debounce';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const shop = $derived(shopSettings(data.config.settings));
	const current = $derived(data.categories.find((category) => category.slug === data.filters.category));
	const sortLabels: Record<string, () => string> = {
		NEWEST: m.shop_sort_newest,
		PRICE_ASC: m.shop_sort_price_asc,
		PRICE_DESC: m.shop_sort_price_desc,
		NAME: m.shop_sort_name,
	};

	function filter(changes: Record<string, string | null>) {
		void goto(withQuery(page.url, { ...changes, page: null }), { keepFocus: true, noScroll: true, replaceState: true });
	}

	const search = debounce((value: string) => filter({ q: value.trim() }));
</script>

<Seo title={current?.name ?? m.shop_all_products()} description={current?.description || undefined} />

<div class="mx-auto max-w-6xl px-4 py-10 sm:px-6">
	<div class="mb-8 space-y-2">
		<h1 class="text-3xl font-semibold tracking-tight">{current?.name ?? m.shop_all_products()}</h1>
		{#if current?.description}
			<p class="max-w-2xl text-muted-foreground">{current.description}</p>
		{/if}
	</div>

	<nav aria-label={m.shop_categories()} class="mb-4">
		<ul class="flex flex-wrap gap-2">
			<li>
				<Button
					size="sm"
					class="rounded-full"
					variant={data.filters.category ? 'outline' : 'default'}
					href={withQuery(page.url, { category: null, page: null })}
				>
					{m.shop_all()}
				</Button>
			</li>
			{#each data.categories as category (category.id)}
				<li>
					<Button
						size="sm"
						class="rounded-full"
						variant={category.slug === data.filters.category ? 'default' : 'outline'}
						aria-current={category.slug === data.filters.category ? 'page' : undefined}
						href={withQuery(page.url, { category: category.slug, page: null })}
					>
						{category.name}
					</Button>
				</li>
			{/each}
		</ul>
	</nav>

	<div class="mb-8 flex flex-wrap items-center gap-3">
		<div class="relative w-full sm:max-w-xs">
			<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
			<Input
				type="search"
				class="ps-9"
				placeholder={m.shop_search()}
				aria-label={m.shop_search()}
				value={data.filters.q}
				oninput={(event) => search(event.currentTarget.value)}
			/>
		</div>
		<Select.Root
			type="single"
			value={data.filters.sort}
			onValueChange={(value) => filter({ sort: value === 'NEWEST' ? null : value })}
		>
			<Select.Trigger class="w-52" aria-label={m.shop_sort()}>
				<span class="text-muted-foreground">{m.shop_sort()}:</span>
				{sortLabels[data.filters.sort]()}
			</Select.Trigger>
			<Select.Content>
				{#each Object.entries(sortLabels) as [value, label] (value)}
					<Select.Item {value} label={label()} />
				{/each}
			</Select.Content>
		</Select.Root>
		<p class="ms-auto text-sm text-muted-foreground">{m.products_count({ count: data.products.totalItems })}</p>
	</div>

	{#if data.products.items.length === 0}
		<Empty.Root class="border border-dashed">
			<Empty.Header>
				<Empty.Media variant="icon"><PackageSearch /></Empty.Media>
				<Empty.Title>{m.shop_no_results()}</Empty.Title>
				<Empty.Description>{m.shop_no_results_body()}</Empty.Description>
			</Empty.Header>
			<Empty.Content>
				<Button variant="outline" href="/products">{m.shop_clear_filters()}</Button>
			</Empty.Content>
		</Empty.Root>
	{:else}
		<ProductGrid products={data.products.items} currency={shop.currency} />
		<div class="mt-10">
			<PaginationBar
				page={data.products.page}
				size={data.products.size}
				totalItems={data.products.totalItems}
				onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
			/>
		</div>
	{/if}
</div>
