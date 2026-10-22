<script lang="ts">
	import Package from '@lucide/svelte/icons/package';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import OrderStatusBadge from '$lib/components/shop/OrderStatusBadge.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import * as Empty from '$lib/components/ui/empty';
	import * as Table from '$lib/components/ui/table';
	import { formatDate, formatMoney } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import { orderNumber } from '$lib/shop/labels';

	let { data } = $props();
</script>

<Seo title={m.orders_title()} />

<PageHeader title={m.orders_title()} description={m.orders_subtitle()}>
	{#snippet actions()}
		<Button variant="outline" href="/products">{m.nav_shop()}</Button>
	{/snippet}
</PageHeader>

{#if data.orders.items.length === 0}
	<Empty.Root class="border border-dashed">
		<Empty.Header>
			<Empty.Media variant="icon"><Package /></Empty.Media>
			<Empty.Title>{m.orders_empty_title()}</Empty.Title>
			<Empty.Description>{m.orders_empty_body()}</Empty.Description>
		</Empty.Header>
		<Empty.Content>
			<Button href="/products">{m.shop_hero_cta()}</Button>
		</Empty.Content>
	</Empty.Root>
{:else}
	<Card.Root class="py-0">
		<Table.Root>
			<Table.Header>
				<Table.Row>
					<Table.Head>{m.orders_col_order()}</Table.Head>
					<Table.Head>{m.orders_col_status()}</Table.Head>
					<Table.Head class="hidden sm:table-cell">{m.orders_col_items()}</Table.Head>
					<Table.Head class="text-end">{m.orders_col_total()}</Table.Head>
				</Table.Row>
			</Table.Header>
			<Table.Body>
				{#each data.orders.items as order (order.id)}
					<Table.Row class="relative">
						<Table.Cell>
							<a href="/orders/{order.id}" class="font-medium after:absolute after:inset-0 hover:underline">
								{orderNumber(order.number)}
							</a>
							<span class="block text-xs text-muted-foreground">{formatDate(order.createdAt)}</span>
						</Table.Cell>
						<Table.Cell><OrderStatusBadge status={order.status} /></Table.Cell>
						<Table.Cell class="hidden text-muted-foreground sm:table-cell">
							{m.items_count({ count: order.itemCount })}
						</Table.Cell>
						<Table.Cell class="text-end font-medium tabular-nums"
							>{formatMoney(order.totalCents, order.currency)}</Table.Cell
						>
					</Table.Row>
				{/each}
			</Table.Body>
		</Table.Root>
	</Card.Root>
	<div class="mt-6">
		<PaginationBar
			page={data.orders.page}
			size={data.orders.size}
			totalItems={data.orders.totalItems}
			onPageChange={(next) => goto(withQuery(page.url, { page: next || null }))}
		/>
	</div>
{/if}
