<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import ClipboardCheck from '@lucide/svelte/icons/clipboard-check';
	import PackageMinus from '@lucide/svelte/icons/package-minus';
	import PackagePlus from '@lucide/svelte/icons/package-plus';
	import Pencil from '@lucide/svelte/icons/pencil';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import AdjustStockDialog from '$lib/components/inventory/AdjustStockDialog.svelte';
	import MovementTable from '$lib/components/inventory/MovementTable.svelte';
	import ProductInventorySettings from '$lib/components/inventory/ProductInventorySettings.svelte';
	import type { AdjustmentKind } from '$lib/components/inventory/types';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import StockStatusBadge from '$lib/components/shop/admin/StockStatusBadge.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { formatMoney, formatNumber } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();

	const row = $derived(data.row);
	let kind = $state<AdjustmentKind>('RECEIVE');
	let open = $state(false);
	let key = $state(0);

	function start(next: AdjustmentKind) {
		kind = next;
		key++;
		open = true;
	}
</script>

<Seo title={m.inventory_of({ name: row.name })} />

<Button variant="ghost" size="sm" href="/admin/inventory" class="-ms-2 mb-4">
	<ArrowLeft class="rtl:rotate-180" />
	{m.inventory_back()}
</Button>

<PageHeader title={row.name} description={row.sku ?? undefined}>
	{#snippet actions()}
		<Button variant="outline" href="/admin/products/{row.productId}"><Pencil />{m.inventory_edit_product()}</Button>
	{/snippet}
</PageHeader>

<div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
	<div class="min-w-0 space-y-6">
		<Card.Root>
			<Card.Content class="flex flex-wrap items-center gap-6">
				<ProductImage src={row.imageUrl} alt="" class="size-20 shrink-0 rounded-lg border" />
				<div class="space-y-1">
					<p class="text-sm text-muted-foreground">{m.inventory_stock()}</p>
					<p class="text-4xl font-semibold tracking-tight tabular-nums">
						{row.trackInventory ? formatNumber(row.stock) : '—'}
					</p>
					<StockStatusBadge status={row.status} />
				</div>
				{#if row.stockValueCents != null}
					<div class="space-y-1">
						<p class="text-sm text-muted-foreground">{m.inventory_value()}</p>
						<p class="text-xl font-semibold tabular-nums">{formatMoney(row.stockValueCents, data.summary.currency)}</p>
					</div>
				{/if}
				<div class="flex flex-wrap gap-2 sm:ms-auto">
					<Button onclick={() => start('RECEIVE')}><PackagePlus />{m.adjust_receive()}</Button>
					<Button variant="outline" onclick={() => start('REMOVE')}><PackageMinus />{m.adjust_remove()}</Button>
					<Button variant="outline" onclick={() => start('COUNT')}><ClipboardCheck />{m.adjust_count()}</Button>
				</div>
			</Card.Content>
		</Card.Root>

		<Card.Root>
			<Card.Header>
				<Card.Title>{m.inventory_history()}</Card.Title>
				<Card.Description>{m.inventory_history_hint()}</Card.Description>
			</Card.Header>
			<Card.Content class="px-2">
				<MovementTable movements={data.movements.items} />
			</Card.Content>
		</Card.Root>
		<PaginationBar
			page={data.movements.page}
			size={data.movements.size}
			totalItems={data.movements.totalItems}
			onPageChange={(next) => goto(withQuery(page.url, { page: next || null }), { noScroll: true })}
		/>
	</div>

	<aside>
		{#key row.version}
			<ProductInventorySettings
				{row}
				currency={data.summary.currency}
				globalThreshold={data.summary.lowStockThreshold}
			/>
		{/key}
	</aside>
</div>

{#key key}
	<AdjustStockDialog bind:open product={row} {kind} />
{/key}
