<script lang="ts">
	import Plus from '@lucide/svelte/icons/plus';
	import Search from '@lucide/svelte/icons/search';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import StockStatusBadge from '$lib/components/shop/admin/StockStatusBadge.svelte';
	import { Badge } from '$lib/components/ui/badge';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import * as Table from '$lib/components/ui/table';
	import { debounce } from '$lib/debounce';
	import { formatMoney, formatNumber } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	const currency = $derived(shopSettings(data.config.settings).currency);
	const ALL = 'ALL';
	const statusLabels: Record<string, () => string> = {
		ALL: m.admin_filter_all,
		ACTIVE: m.product_status_active,
		INACTIVE: m.product_status_inactive,
		LOW_STOCK: m.product_status_low_stock,
		OUT_OF_STOCK: m.shop_out_of_stock,
	};
	const categoryName = $derived(
		data.categories.find((category) => category.id === data.filters.categoryId)?.name ?? m.admin_filter_all(),
	);

	function filter(changes: Record<string, string | null>) {
		void goto(withQuery(page.url, { ...changes, page: null }), { keepFocus: true, noScroll: true, replaceState: true });
	}

	const search = debounce((value: string) => filter({ q: value.trim() }));
</script>

<Seo title={m.admin_products_title()} />

<PageHeader title={m.admin_products_title()} description={m.admin_products_subtitle()}>
	{#snippet actions()}
		<Button href="/admin/products/new"><Plus />{m.admin_product_new()}</Button>
	{/snippet}
</PageHeader>

<div class="mb-4 flex flex-wrap items-center gap-3">
	<div class="relative w-full sm:max-w-xs">
		<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
		<Input
			type="search"
			class="ps-9"
			placeholder={m.admin_products_search()}
			aria-label={m.admin_products_search()}
			value={data.filters.q}
			oninput={(event) => search(event.currentTarget.value)}
		/>
	</div>
	<Select.Root
		type="single"
		value={data.filters.status ?? ALL}
		onValueChange={(value) => filter({ status: value === ALL ? null : value })}
	>
		<Select.Trigger class="w-48" aria-label={m.admin_filter_status()}>
			<span class="text-muted-foreground">{m.admin_filter_status()}:</span>
			{statusLabels[data.filters.status ?? ALL]()}
		</Select.Trigger>
		<Select.Content>
			{#each Object.entries(statusLabels) as [value, label] (value)}
				<Select.Item {value} label={label()} />
			{/each}
		</Select.Content>
	</Select.Root>
	<Select.Root
		type="single"
		value={data.filters.categoryId ?? ALL}
		onValueChange={(value) => filter({ category: value === ALL ? null : value })}
	>
		<Select.Trigger class="w-52" aria-label={m.product_field_category()}>
			<span class="text-muted-foreground">{m.product_field_category()}:</span>
			<span class="truncate">{categoryName}</span>
		</Select.Trigger>
		<Select.Content>
			<Select.Item value={ALL} label={m.admin_filter_all()} />
			{#each data.categories as category (category.id)}
				<Select.Item value={category.id} label={category.name} />
			{/each}
		</Select.Content>
	</Select.Root>
	<p class="ms-auto text-sm text-muted-foreground">{m.products_count({ count: data.products.totalItems })}</p>
</div>

<Card.Root class="py-0">
	<Table.Root>
		<Table.Header>
			<Table.Row>
				<Table.Head>{m.order_col_product()}</Table.Head>
				<Table.Head class="text-end">{m.order_col_price()}</Table.Head>
				<Table.Head class="text-end">{m.product_field_stock()}</Table.Head>
				<Table.Head class="hidden md:table-cell">{m.admin_col_status()}</Table.Head>
			</Table.Row>
		</Table.Header>
		<Table.Body>
			{#each data.products.items as product (product.id)}
				<Table.Row class="relative">
					<Table.Cell>
						<div class="flex items-center gap-3">
							<ProductImage src={product.imageUrl} alt="" class="size-10 shrink-0 rounded-md border" />
							<div class="min-w-0">
								<a
									href="/admin/products/{product.id}"
									class="block truncate font-medium after:absolute after:inset-0 hover:underline"
								>
									{product.name}
								</a>
								<span class="block truncate text-xs text-muted-foreground">
									{product.category?.name ?? m.product_no_category()}
									{#if product.sku}· <span dir="ltr">{product.sku}</span>{/if}
								</span>
							</div>
						</div>
					</Table.Cell>
					<Table.Cell class="text-end tabular-nums">{formatMoney(product.priceCents, currency)}</Table.Cell>
					<Table.Cell class="text-end tabular-nums">
						{product.trackInventory ? formatNumber(product.stock) : '—'}
					</Table.Cell>
					<Table.Cell class="hidden md:table-cell">
						<div class="flex flex-wrap gap-1">
							{#if product.active}
								<Badge variant="outline">{m.product_status_active()}</Badge>
							{:else}
								<Badge variant="secondary">{m.product_status_inactive()}</Badge>
							{/if}
							{#if product.featured}<Badge variant="outline">{m.product_featured()}</Badge>{/if}
							{#if product.stockStatus === 'LOW' || product.stockStatus === 'OUT'}
								<StockStatusBadge status={product.stockStatus} />
							{/if}
						</div>
					</Table.Cell>
				</Table.Row>
			{:else}
				<Table.Row>
					<Table.Cell colspan={4} class="h-24 text-center text-muted-foreground">{m.shop_no_results()}</Table.Cell>
				</Table.Row>
			{/each}
		</Table.Body>
	</Table.Root>
</Card.Root>

<div class="mt-6">
	<PaginationBar
		page={data.products.page}
		size={data.products.size}
		totalItems={data.products.totalItems}
		onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
	/>
</div>
