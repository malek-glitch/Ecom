<script lang="ts">
	import Search from '@lucide/svelte/icons/search';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import PaginationBar from '$lib/components/app/PaginationBar.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import OrderStatusBadge from '$lib/components/shop/OrderStatusBadge.svelte';
	import * as Card from '$lib/components/ui/card';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import * as Table from '$lib/components/ui/table';
	import { debounce } from '$lib/debounce';
	import { formatDateTime, formatMoney, formatRelative } from '$lib/i18n/format';
	import { withQuery } from '$lib/navigation';
	import { m } from '$lib/paraglide/messages';
	import { ORDER_STATUSES, orderNumber, orderStatusLabel, paymentMethodLabel } from '$lib/shop/labels';

	let { data } = $props();

	const ALL = 'ALL';

	function filter(changes: Record<string, string | null>) {
		void goto(withQuery(page.url, { ...changes, page: null }), { keepFocus: true, noScroll: true, replaceState: true });
	}

	const search = debounce((value: string) => filter({ q: value.trim() }));
</script>

<Seo title={m.admin_orders_title()} />

<PageHeader title={m.admin_orders_title()} description={m.admin_orders_subtitle()} />

<div class="mb-4 flex flex-wrap items-center gap-3">
	<div class="relative w-full sm:max-w-xs">
		<Search class="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
		<Input
			type="search"
			class="ps-9"
			placeholder={m.admin_orders_search()}
			aria-label={m.admin_orders_search()}
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
			{data.filters.status ? orderStatusLabel(data.filters.status) : m.admin_filter_all()}
		</Select.Trigger>
		<Select.Content>
			<Select.Item value={ALL} label={m.admin_filter_all()} />
			{#each ORDER_STATUSES as status (status)}
				<Select.Item value={status} label={orderStatusLabel(status)} />
			{/each}
		</Select.Content>
	</Select.Root>
	<p class="ms-auto text-sm text-muted-foreground">{m.orders_count({ count: data.orders.totalItems })}</p>
</div>

<Card.Root class="py-0">
	<Table.Root>
		<Table.Header>
			<Table.Row>
				<Table.Head>{m.orders_col_order()}</Table.Head>
				<Table.Head>{m.orders_col_customer()}</Table.Head>
				<Table.Head>{m.orders_col_status()}</Table.Head>
				<Table.Head class="hidden lg:table-cell">{m.checkout_payment()}</Table.Head>
				<Table.Head class="text-end">{m.orders_col_total()}</Table.Head>
			</Table.Row>
		</Table.Header>
		<Table.Body>
			{#each data.orders.items as order (order.id)}
				<Table.Row class="relative">
					<Table.Cell>
						<a href="/admin/orders/{order.id}" class="font-medium after:absolute after:inset-0 hover:underline">
							{orderNumber(order.number)}
						</a>
						<span class="block text-xs text-muted-foreground" title={formatDateTime(order.createdAt)}>
							{formatRelative(order.createdAt)}
						</span>
					</Table.Cell>
					<Table.Cell>
						<span class="block truncate">{order.customerName}</span>
						<span class="block truncate text-xs text-muted-foreground" dir="ltr">{order.email}</span>
					</Table.Cell>
					<Table.Cell><OrderStatusBadge status={order.status} /></Table.Cell>
					<Table.Cell class="hidden text-muted-foreground lg:table-cell"
						>{paymentMethodLabel(order.paymentMethod)}</Table.Cell
					>
					<Table.Cell class="text-end font-medium tabular-nums"
						>{formatMoney(order.totalCents, order.currency)}</Table.Cell
					>
				</Table.Row>
			{:else}
				<Table.Row>
					<Table.Cell colspan={5} class="h-24 text-center text-muted-foreground">{m.admin_orders_empty()}</Table.Cell>
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
