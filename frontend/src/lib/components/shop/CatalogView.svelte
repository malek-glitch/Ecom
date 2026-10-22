<script lang="ts">
	import PackageSearch from '@lucide/svelte/icons/package-search';
	import SlidersHorizontal from '@lucide/svelte/icons/sliders-horizontal';
	import Sparkles from '@lucide/svelte/icons/sparkles';
	import type { Snippet } from 'svelte';
	import { afterNavigate, goto } from '$app/navigation';
	import { page } from '$app/state';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Empty from '$lib/components/ui/empty';
	import * as Select from '$lib/components/ui/select';
	import * as Sheet from '$lib/components/ui/sheet';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import type { CatalogFilters as Filters, SearchResults, Sort } from '$lib/shop/catalog';
	import CatalogFilters from './CatalogFilters.svelte';
	import ProductGrid from './ProductGrid.svelte';

	/** The product list shared by /products, /categories/<slug> and search results. */
	let {
		results,
		filters,
		currency,
		category,
		header,
	}: { results: SearchResults; filters: Filters; currency: string; category?: string; header: Snippet } = $props();

	let filtersOpen = $state(false);
	afterNavigate(() => (filtersOpen = false));

	const sortLabels: Record<Sort, () => string> = {
		RELEVANCE: m.shop_sort_relevance,
		NEWEST: m.shop_sort_newest,
		PRICE_ASC: m.shop_sort_price_asc,
		PRICE_DESC: m.shop_sort_price_desc,
		NAME: m.shop_sort_name,
	};
	const defaultSort: Sort = $derived(filters.q ? 'RELEVANCE' : 'NEWEST');
	const sorts = $derived((Object.keys(sortLabels) as Sort[]).filter((sort) => sort !== 'RELEVANCE' || filters.q));

	function sortBy(value: string) {
		void goto(withQuery(page.url, { sort: value === defaultSort ? null : value, page: null }), { noScroll: true });
	}
</script>

<div class="mx-auto max-w-7xl px-4 py-8 sm:px-6">
	{@render header()}

	{#if results.didYouMean}
		<p class="mb-6 flex flex-wrap items-center gap-2 rounded-lg border bg-muted/40 px-4 py-3 text-sm">
			<Sparkles class="size-4 text-primary" />
			{m.search_did_you_mean()}
			<a
				href={withQuery(page.url, { q: results.didYouMean, page: null })}
				class="font-semibold text-primary underline-offset-4 hover:underline"
			>
				{results.didYouMean}
			</a>
		</p>
	{/if}

	<div class="grid gap-8 lg:grid-cols-[15rem_minmax(0,1fr)]">
		<aside class="hidden lg:block" aria-label={m.filter_title()}>
			<div class="sticky top-24">
				<CatalogFilters {results} {filters} {currency} {category} />
			</div>
		</aside>

		<section aria-label={m.shop_results()}>
			<div class="mb-6 flex flex-wrap items-center gap-3">
				<Button variant="outline" size="sm" class="lg:hidden" onclick={() => (filtersOpen = true)}>
					<SlidersHorizontal />{m.filter_title()}
				</Button>
				<p class="text-sm text-muted-foreground" aria-live="polite">
					{m.products_count({ count: results.totalItems })}
				</p>
				<Select.Root type="single" value={results.sort} onValueChange={sortBy}>
					<Select.Trigger class="ms-auto w-56" aria-label={m.shop_sort()}>
						<span class="text-muted-foreground">{m.shop_sort()}:</span>
						{sortLabels[results.sort]()}
					</Select.Trigger>
					<Select.Content>
						{#each sorts as sort (sort)}
							<Select.Item value={sort} label={sortLabels[sort]()} />
						{/each}
					</Select.Content>
				</Select.Root>
			</div>

			{#if results.items.length === 0}
				<Empty.Root class="border border-dashed">
					<Empty.Header>
						<Empty.Media variant="icon"><PackageSearch /></Empty.Media>
						<Empty.Title>{filters.q ? m.search_no_results({ query: filters.q }) : m.shop_no_results()}</Empty.Title>
						<Empty.Description>{m.shop_no_results_body()}</Empty.Description>
					</Empty.Header>
					<Empty.Content>
						<Button variant="outline" href="/products">{m.shop_clear_filters()}</Button>
					</Empty.Content>
				</Empty.Root>
			{:else}
				<ProductGrid products={results.items} {currency} query={filters.q} class="lg:grid-cols-3 xl:grid-cols-4" />
				<div class="mt-10">
					<PaginationBar
						page={results.page}
						size={results.size}
						totalItems={results.totalItems}
						onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
					/>
				</div>
			{/if}
		</section>
	</div>
</div>

<Sheet.Root bind:open={filtersOpen}>
	<Sheet.Content side="left" class="overflow-y-auto">
		<Sheet.Header>
			<Sheet.Title>{m.filter_title()}</Sheet.Title>
		</Sheet.Header>
		<div class="px-4 pb-6">
			<CatalogFilters {results} {filters} {currency} {category} onApply={() => (filtersOpen = false)} />
		</div>
	</Sheet.Content>
</Sheet.Root>
