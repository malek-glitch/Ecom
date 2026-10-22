<script lang="ts">
	import AlertTriangle from '@lucide/svelte/icons/triangle-alert';
	import Boxes from '@lucide/svelte/icons/boxes';
	import Download from '@lucide/svelte/icons/download';
	import PackageMinus from '@lucide/svelte/icons/package-minus';
	import PackageX from '@lucide/svelte/icons/package-x';
	import Search from '@lucide/svelte/icons/search';
	import Wallet from '@lucide/svelte/icons/wallet';
	import { toast } from 'svelte-sonner';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import AdjustStockDialog from '$lib/components/inventory/AdjustStockDialog.svelte';
	import InventoryModuleCard from '$lib/components/inventory/InventoryModuleCard.svelte';
	import MovementTable from '$lib/components/inventory/MovementTable.svelte';
	import type { InventoryRow } from '$lib/components/inventory/types';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import StockStatusBadge from '$lib/components/shop/admin/StockStatusBadge.svelte';
	import * as Alert from '$lib/components/ui/alert';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Input } from '$lib/components/ui/input';
	import * as Table from '$lib/components/ui/table';
	import * as Tabs from '$lib/components/ui/tabs';
	import { debounce } from '$lib/debounce';
	import { formatMoney, formatNumber } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import { cn } from '$lib/utils';

	let { data } = $props();

	const summary = $derived(data.summary);
	let adjusting = $state<InventoryRow | null>(null);
	let adjustOpen = $state(false);
	let adjustKey = $state(0);
	let exporting = $state(false);

	const cards = $derived([
		{ label: m.inventory_units(), value: formatNumber(summary.unitsInStock), icon: Boxes, href: null },
		{
			label: m.stock_status_low(),
			value: formatNumber(summary.lowStock),
			icon: PackageMinus,
			href: '/admin/inventory?status=LOW',
			alert: summary.lowStock > 0,
		},
		{
			label: m.shop_out_of_stock(),
			value: formatNumber(summary.outOfStock),
			icon: PackageX,
			href: '/admin/inventory?status=OUT',
			alert: summary.outOfStock > 0,
		},
		{
			label: m.inventory_value(),
			value: formatMoney(summary.stockValueCents, summary.currency),
			icon: Wallet,
			href: null,
			hint: m.inventory_retail_value({ amount: formatMoney(summary.retailValueCents, summary.currency) }),
		},
	]);

	const tabs = [
		{ value: 'ALL', label: m.admin_filter_all },
		{ value: 'LOW', label: m.stock_status_low },
		{ value: 'OUT', label: m.shop_out_of_stock },
		{ value: 'IN_STOCK', label: m.stock_status_in_stock },
		{ value: 'UNTRACKED', label: m.stock_status_untracked },
	];

	function filter(changes: Record<string, string | null>) {
		void goto(withQuery(page.url, { ...changes, page: null }), { keepFocus: true, noScroll: true, replaceState: true });
	}
	const search = debounce((value: string) => filter({ q: value.trim() }));

	function adjust(row: InventoryRow) {
		adjusting = row;
		adjustKey++;
		adjustOpen = true;
	}

	/** The CSV comes from the API with the admin's token, then is handed to the browser as a download. */
	async function exportCsv() {
		exporting = true;
		try {
			const csv = await call(api.GET('/api/v1/admin/inventory/export', { parseAs: 'text' }));
			const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
			const link = document.createElement('a');
			link.href = url;
			link.download = `stock-${new Date().toISOString().slice(0, 10)}.csv`;
			link.click();
			URL.revokeObjectURL(url);
		} catch (error) {
			toast.error(errorMessage(error));
		} finally {
			exporting = false;
		}
	}
</script>

<Seo title={m.nav_admin_inventory()} />

<PageHeader title={m.nav_admin_inventory()} description={m.inventory_subtitle()}>
	{#snippet actions()}
		<Button variant="outline" disabled={exporting} onclick={exportCsv}><Download />{m.inventory_export()}</Button>
	{/snippet}
</PageHeader>

{#if !summary.enabled}
	<Alert.Root class="mb-6">
		<AlertTriangle />
		<Alert.Title>{m.inventory_off_title()}</Alert.Title>
		<Alert.Description>{m.inventory_off_body()}</Alert.Description>
	</Alert.Root>
{/if}

<div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
	{#each cards as card (card.label)}
		{@const Icon = card.icon}
		<svelte:element
			this={card.href ? 'a' : 'div'}
			href={card.href}
			class="group rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
		>
			<Card.Root class={cn('h-full', card.href && 'transition-colors group-hover:border-primary/40')}>
				<Card.Header>
					<Card.Description class="flex items-center gap-2">
						<Icon class={cn('size-4', card.alert && 'text-destructive')} />{card.label}
					</Card.Description>
					<Card.Title class="text-3xl tabular-nums">{card.value}</Card.Title>
					{#if card.hint}<p class="text-xs text-muted-foreground">{card.hint}</p>{/if}
				</Card.Header>
			</Card.Root>
		</svelte:element>
	{/each}
</div>
{#if summary.productsWithoutCost > 0}
	<p class="-mt-3 mb-6 text-xs text-muted-foreground">
		{m.inventory_missing_cost({ count: summary.productsWithoutCost })}
	</p>
{/if}

<div class="space-y-6">
	<div class="min-w-0 space-y-4">
		<div class="flex flex-wrap items-center gap-3">
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
			<Tabs.Root
				value={data.filters.status ?? 'ALL'}
				onValueChange={(value) => filter({ status: value === 'ALL' ? null : value })}
			>
				<Tabs.List class="flex-wrap">
					{#each tabs as tab (tab.value)}
						<Tabs.Trigger value={tab.value}>{tab.label()}</Tabs.Trigger>
					{/each}
				</Tabs.List>
			</Tabs.Root>
		</div>

		<Card.Root class="py-0">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>{m.order_col_product()}</Table.Head>
						<Table.Head>{m.admin_col_status()}</Table.Head>
						<Table.Head class="text-end">{m.inventory_stock()}</Table.Head>
						<Table.Head class="hidden text-end lg:table-cell">{m.inventory_threshold_short()}</Table.Head>
						<Table.Head class="hidden text-end md:table-cell">{m.inventory_value()}</Table.Head>
						<Table.Head class="w-24"><span class="sr-only">{m.common_actions()}</span></Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each data.products.items as row (row.productId)}
						<Table.Row>
							<Table.Cell>
								<div class="flex items-center gap-3">
									<ProductImage src={row.imageUrl} alt="" class="size-10 shrink-0 rounded-md border" />
									<div class="min-w-0">
										<a href="/admin/inventory/{row.productId}" class="block truncate font-medium hover:underline"
											>{row.name}</a
										>
										<span class="block truncate text-xs text-muted-foreground" dir="ltr">{row.sku ?? ''}</span>
									</div>
								</div>
							</Table.Cell>
							<Table.Cell><StockStatusBadge status={row.status} /></Table.Cell>
							<Table.Cell class="text-end font-medium tabular-nums"
								>{row.trackInventory ? formatNumber(row.stock) : '—'}</Table.Cell
							>
							<Table.Cell class="hidden text-end text-muted-foreground tabular-nums lg:table-cell">
								{row.trackInventory ? row.lowStockThreshold : '—'}
							</Table.Cell>
							<Table.Cell class="hidden text-end text-muted-foreground tabular-nums md:table-cell">
								{row.stockValueCents != null ? formatMoney(row.stockValueCents, summary.currency) : '—'}
							</Table.Cell>
							<Table.Cell class="text-end">
								<Button variant="outline" size="sm" onclick={() => adjust(row)}>{m.inventory_adjust()}</Button>
							</Table.Cell>
						</Table.Row>
					{:else}
						<Table.Row>
							<Table.Cell colspan={6} class="h-24 text-center text-muted-foreground">{m.shop_no_results()}</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</Card.Root>
		<PaginationBar
			page={data.products.page}
			size={data.products.size}
			totalItems={data.products.totalItems}
			onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
		/>
	</div>

	<div class="grid gap-6 lg:grid-cols-[22rem_minmax(0,1fr)]">
		<InventoryModuleCard {summary} />
		<Card.Root>
			<Card.Header>
				<Card.Title>{m.inventory_recent()}</Card.Title>
			</Card.Header>
			<Card.Content class="px-2">
				<MovementTable movements={data.movements.items} showProduct />
			</Card.Content>
		</Card.Root>
	</div>
</div>

{#if adjusting}
	{#key adjustKey}
		<AdjustStockDialog bind:open={adjustOpen} product={adjusting} />
	{/key}
{/if}
