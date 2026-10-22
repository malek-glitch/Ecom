<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { Button } from '$lib/components/ui/button';
	import { Checkbox } from '$lib/components/ui/checkbox';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import { currencyDigits, formatMoney } from '$lib/i18n/format';
	import { queryWithout, withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import type { CatalogFilters, SearchResults } from '$lib/shop/catalog';
	import { cn } from '$lib/utils';

	/** Category, price and stock filters. Every choice is a URL, so filtered pages can be shared. */
	let {
		results,
		filters,
		currency,
		category,
		onApply,
	}: {
		results: SearchResults;
		filters: CatalogFilters;
		currency: string;
		/** The category page being shown, if any. */
		category?: string;
		onApply?: () => void;
	} = $props();

	const id = $props.id();
	// Follow the URL, but let the shopper type freely until they apply.
	let min = $derived(filters.min?.toString() ?? '');
	let max = $derived(filters.max?.toString() ?? '');

	/** The current filters minus the page (a new filter starts at page 1), for another category's URL. */
	const keep = () => queryWithout(page.url, 'page', 'category');

	function apply(changes: Record<string, string | null>) {
		onApply?.();
		void goto(withQuery(page.url, { ...changes, page: null }), { noScroll: true, keepFocus: true });
	}

	function applyPrice(event: Event) {
		event.preventDefault();
		const clean = (value: string) => {
			const n = Number(value.replace(',', '.'));
			return value.trim() && Number.isFinite(n) && n >= 0 ? String(n) : null;
		};
		apply({ min: clean(min), max: clean(max) });
	}

	const digits = $derived(currencyDigits(currency));
	const hasFilters = $derived(filters.min !== undefined || filters.max !== undefined || filters.inStock);
</script>

<div class="space-y-8">
	<section aria-labelledby="{id}-categories">
		<h2 id="{id}-categories" class="mb-3 text-sm font-semibold">{m.shop_categories()}</h2>
		<ul class="space-y-0.5 text-sm">
			<li>
				<a
					href="/products{keep()}"
					class={cn(
						'flex justify-between rounded-md px-2 py-1.5 hover:bg-accent',
						!category && 'bg-accent font-medium',
					)}
					aria-current={!category ? 'page' : undefined}
				>
					{m.shop_all()}
				</a>
			</li>
			{#each results.categories as facet (facet.slug)}
				<li>
					<a
						href="/categories/{facet.slug}{keep()}"
						class={cn(
							'flex justify-between gap-2 rounded-md px-2 py-1.5 hover:bg-accent',
							facet.slug === category && 'bg-accent font-medium',
						)}
						aria-current={facet.slug === category ? 'page' : undefined}
					>
						<span class="truncate">{facet.name}</span>
						<span class="text-muted-foreground tabular-nums">{facet.count}</span>
					</a>
				</li>
			{/each}
		</ul>
	</section>

	<section aria-labelledby="{id}-price">
		<h2 id="{id}-price" class="mb-3 text-sm font-semibold">{m.filter_price({ currency })}</h2>
		<form class="space-y-2" onsubmit={applyPrice}>
			<div class="flex items-center gap-2">
				<Input
					aria-label={m.filter_min()}
					placeholder={results.priceRange
						? String(Math.floor(results.priceRange.minCents / 10 ** digits))
						: m.filter_min()}
					inputmode="decimal"
					dir="ltr"
					bind:value={min}
				/>
				<span class="text-muted-foreground">–</span>
				<Input
					aria-label={m.filter_max()}
					placeholder={results.priceRange
						? String(Math.ceil(results.priceRange.maxCents / 10 ** digits))
						: m.filter_max()}
					inputmode="decimal"
					dir="ltr"
					bind:value={max}
				/>
			</div>
			{#if results.priceRange}
				<p class="text-xs text-muted-foreground">
					{m.filter_price_range({
						min: formatMoney(results.priceRange.minCents, currency),
						max: formatMoney(results.priceRange.maxCents, currency),
					})}
				</p>
			{/if}
			<Button type="submit" variant="outline" size="sm" class="w-full">{m.filter_apply()}</Button>
		</form>
	</section>

	<section class="flex items-center gap-2">
		<Checkbox
			id="{id}-stock"
			checked={filters.inStock}
			onCheckedChange={(checked) => apply({ stock: checked ? '1' : null })}
		/>
		<Label for="{id}-stock" class="font-normal">{m.filter_in_stock()}</Label>
	</section>

	{#if hasFilters}
		<Button variant="ghost" size="sm" class="-ms-2" onclick={() => apply({ min: null, max: null, stock: null })}>
			{m.filter_clear()}
		</Button>
	{/if}
</div>
