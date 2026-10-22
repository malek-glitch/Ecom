<script lang="ts">
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import FolderOpen from '@lucide/svelte/icons/folder-open';
	import History from '@lucide/svelte/icons/history';
	import Search from '@lucide/svelte/icons/search';
	import Sparkles from '@lucide/svelte/icons/sparkles';
	import X from '@lucide/svelte/icons/x';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api, call } from '$lib/api/client';
	import type { components } from '$lib/api/schema';
	import { Spinner } from '$lib/components/ui/spinner';
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { clearRecentSearches, recentSearches, rememberSearch } from '$lib/shop/recent-searches';
	import { shopSettings } from '$lib/shop/store-api';
	import { cn } from '$lib/utils';
	import Highlight from './Highlight.svelte';
	import ProductImage from './ProductImage.svelte';

	/**
	 * The store search box: suggestions while typing (products, categories, a correction for
	 * misspellings), recent searches when empty, full keyboard support (↑ ↓ Enter Esc).
	 */
	let {
		autofocus = false,
		class: className = '',
		onNavigate,
	}: { autofocus?: boolean; class?: string; onNavigate?: () => void } = $props();

	type Suggestions = components['schemas']['SearchSuggestions'];
	interface Option {
		id: string;
		href: string;
		query?: string;
	}

	const id = $props.id();
	const DEBOUNCE_MS = 180;

	let query = $state(page.url.pathname === '/products' ? (page.url.searchParams.get('q') ?? '') : '');
	let open = $state(false);
	let loading = $state(false);
	let suggestions = $state<Suggestions | null>(null);
	let recent = $state<string[]>([]);
	let active = $state(-1);
	let input = $state<HTMLInputElement>();
	let sequence = 0;
	let timer: ReturnType<typeof setTimeout> | undefined;

	const currency = $derived(shopSettings(page.data.config?.settings).currency);
	const trimmed = $derived(query.trim());
	const searchHref = (q: string) => `/products?q=${encodeURIComponent(q)}`;

	/** Every selectable row, in display order, for keyboard navigation. */
	const options = $derived.by<Option[]>(() => {
		if (!trimmed) return recent.map((q, i) => ({ id: `${id}-recent-${i}`, href: searchHref(q), query: q }));
		const s = suggestions;
		if (!s) return [];
		return [
			...(s.didYouMean ? [{ id: `${id}-dym`, href: searchHref(s.didYouMean), query: s.didYouMean }] : []),
			...s.categories.map((c) => ({ id: `${id}-cat-${c.id}`, href: `/categories/${c.slug}` })),
			...s.products.map((p) => ({ id: `${id}-p-${p.id}`, href: `/products/${p.slug}` })),
			{ id: `${id}-all`, href: searchHref(trimmed), query: trimmed },
		];
	});
	const expanded = $derived(open && (trimmed ? suggestions !== null : recent.length > 0));
	const index = (optionId: string) => options.findIndex((option) => option.id === optionId);

	// Opened on purpose (the phone search dialog): ready to type.
	$effect(() => {
		if (autofocus) input?.focus();
	});

	function fetchSuggestions(q: string) {
		clearTimeout(timer);
		const current = ++sequence;
		active = -1;
		if (!q) {
			suggestions = null;
			loading = false;
			return;
		}
		loading = true;
		timer = setTimeout(async () => {
			try {
				const result = await call(api.GET('/api/v1/public/shop/suggest', { params: { query: { q } } }));
				if (current === sequence) suggestions = result;
			} catch {
				if (current === sequence) suggestions = null;
			} finally {
				if (current === sequence) loading = false;
			}
		}, DEBOUNCE_MS);
	}

	function focus() {
		recent = recentSearches();
		open = true;
	}

	async function go(option: Option | { href: string; query?: string }) {
		rememberSearch(option.query ?? trimmed);
		open = false;
		input?.blur();
		onNavigate?.();
		await goto(option.href);
	}

	function submit(event: Event) {
		event.preventDefault();
		const chosen = options[active];
		if (chosen) return void go(chosen);
		if (trimmed) void go({ href: searchHref(trimmed), query: trimmed });
	}

	function keydown(event: KeyboardEvent) {
		if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
			event.preventDefault();
			open = true;
			if (!options.length) return;
			const step = event.key === 'ArrowDown' ? 1 : -1;
			active = (active + step + options.length + 1) % (options.length + 1);
			if (active === options.length) active = -1; // back in the input
		} else if (event.key === 'Escape') {
			if (open) {
				event.preventDefault();
				open = false;
				active = -1;
			} else query = '';
		}
	}

	function clear() {
		query = '';
		fetchSuggestions('');
		input?.focus();
	}
</script>

<form role="search" class={cn('relative w-full', className)} onsubmit={submit} action="/products">
	<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
	<input
		bind:this={input}
		bind:value={query}
		name="q"
		type="search"
		role="combobox"
		autocomplete="off"
		spellcheck="false"
		enterkeyhint="search"
		aria-label={m.search_label()}
		aria-expanded={expanded}
		aria-controls="{id}-listbox"
		aria-autocomplete="list"
		aria-activedescendant={active >= 0 ? options[active]?.id : undefined}
		placeholder={m.search_placeholder()}
		class="h-9 w-full rounded-md border border-input bg-background ps-9 pe-9 text-sm shadow-xs outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 dark:bg-input/30 [&::-webkit-search-cancel-button]:hidden"
		oninput={() => {
			open = true;
			fetchSuggestions(query.trim());
		}}
		onfocus={focus}
		onblur={() => (open = false)}
		onkeydown={keydown}
	/>
	<span class="absolute end-2 top-1/2 flex -translate-y-1/2 items-center">
		{#if loading}
			<Spinner class="size-4 text-muted-foreground" />
		{:else if query}
			<button
				type="button"
				class="grid size-6 place-items-center rounded text-muted-foreground hover:text-foreground"
				aria-label={m.search_clear()}
				onmousedown={(event) => event.preventDefault()}
				onclick={clear}
			>
				<X class="size-4" />
			</button>
		{/if}
	</span>

	{#if expanded}
		<!-- mousedown is prevented so clicking a suggestion doesn't blur (and close) first. -->
		<div
			id="{id}-listbox"
			role="listbox"
			tabindex="-1"
			aria-label={m.search_suggestions()}
			class="absolute inset-x-0 top-full z-50 mt-2 max-h-[min(70vh,32rem)] overflow-y-auto rounded-lg border bg-popover p-1.5 text-popover-foreground shadow-lg"
			onmousedown={(event) => event.preventDefault()}
		>
			{#if !trimmed}
				<div class="flex items-center justify-between px-2 py-1.5 text-xs text-muted-foreground">
					<span>{m.search_recent()}</span>
					<button
						type="button"
						class="hover:text-foreground"
						onclick={() => {
							clearRecentSearches();
							recent = [];
						}}
					>
						{m.search_clear_recent()}
					</button>
				</div>
				{#each recent as q, i (q)}
					{@const option = options[i]}
					<a
						id={option.id}
						href={option.href}
						role="option"
						aria-selected={active === i}
						class={cn(
							'flex items-center gap-2 rounded-md px-2 py-1.5 text-sm',
							active === i && 'bg-accent text-accent-foreground',
						)}
						onclick={(event) => {
							event.preventDefault();
							void go(option);
						}}
					>
						<History class="size-4 text-muted-foreground" />{q}
					</a>
				{/each}
			{:else if suggestions}
				{#if suggestions.didYouMean}
					{@const option = options[index(`${id}-dym`)]}
					<a
						id={option.id}
						href={option.href}
						role="option"
						aria-selected={active === index(option.id)}
						class={cn(
							'flex items-center gap-2 rounded-md px-2 py-2 text-sm',
							active === index(option.id) && 'bg-accent text-accent-foreground',
						)}
						onclick={(event) => {
							event.preventDefault();
							void go(option);
						}}
					>
						<Sparkles class="size-4 text-primary" />
						{m.search_did_you_mean()} <strong>{suggestions.didYouMean}</strong>
					</a>
				{/if}
				{#if suggestions.categories.length}
					<p class="px-2 pt-2 pb-1 text-xs text-muted-foreground">{m.shop_categories()}</p>
					{#each suggestions.categories as category (category.id)}
						{@const optionId = `${id}-cat-${category.id}`}
						<a
							id={optionId}
							href="/categories/{category.slug}"
							role="option"
							aria-selected={active === index(optionId)}
							class={cn(
								'flex items-center gap-2 rounded-md px-2 py-1.5 text-sm',
								active === index(optionId) && 'bg-accent text-accent-foreground',
							)}
							onclick={(event) => {
								event.preventDefault();
								void go({ href: `/categories/${category.slug}` });
							}}
						>
							<FolderOpen class="size-4 text-muted-foreground" /><Highlight text={category.name} query={trimmed} />
						</a>
					{/each}
				{/if}
				{#if suggestions.products.length}
					<p class="px-2 pt-2 pb-1 text-xs text-muted-foreground">{m.search_products()}</p>
					{#each suggestions.products as product (product.id)}
						{@const optionId = `${id}-p-${product.id}`}
						<a
							id={optionId}
							href="/products/{product.slug}"
							role="option"
							aria-selected={active === index(optionId)}
							class={cn(
								'flex items-center gap-3 rounded-md px-2 py-1.5 text-sm',
								active === index(optionId) && 'bg-accent text-accent-foreground',
							)}
							onclick={(event) => {
								event.preventDefault();
								void go({ href: `/products/${product.slug}` });
							}}
						>
							<ProductImage src={product.imageUrl} alt="" class="size-10 shrink-0 rounded-md border" />
							<span class="min-w-0 flex-1">
								<span class="line-clamp-1"><Highlight text={product.name} query={trimmed} /></span>
								{#if product.category}<span class="text-xs text-muted-foreground">{product.category.name}</span>{/if}
							</span>
							<span class="shrink-0 font-medium tabular-nums">{formatMoney(product.priceCents, currency)}</span>
						</a>
					{/each}
				{:else if !suggestions.didYouMean}
					<p class="px-2 py-3 text-sm text-muted-foreground">{m.search_no_match({ query: trimmed })}</p>
				{/if}
				{@const allIndex = index(`${id}-all`)}
				<a
					id="{id}-all"
					href={searchHref(trimmed)}
					role="option"
					aria-selected={active === allIndex}
					class={cn(
						'mt-1 flex items-center justify-between gap-2 rounded-md border-t px-2 py-2 text-sm font-medium',
						active === allIndex && 'bg-accent text-accent-foreground',
					)}
					onclick={(event) => {
						event.preventDefault();
						void go({ href: searchHref(trimmed), query: trimmed });
					}}
				>
					{m.search_see_all({ count: suggestions.totalProducts, query: trimmed })}
					<ArrowRight class="size-4 rtl:rotate-180" />
				</a>
			{/if}
		</div>
	{/if}
</form>
