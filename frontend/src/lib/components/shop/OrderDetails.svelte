<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { components } from '$lib/api/schema';
	import * as Card from '$lib/components/ui/card';
	import * as Table from '$lib/components/ui/table';
	import { formatDateTime, formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { orderNumber, paymentMethodLabel } from '$lib/shop/labels';
	import OrderStatusBadge from './OrderStatusBadge.svelte';
	import OrderTotals from './OrderTotals.svelte';

	/** One order, as the customer and the admin both see it. `actions` go in the header. */
	let {
		order,
		actions,
		productHref = (slug: string) => `/products/${slug}`,
	}: {
		order: components['schemas']['OrderResponse'];
		actions?: Snippet;
		productHref?: (slug: string, productId?: string | null) => string | null;
	} = $props();

	const address = $derived(order.shippingAddress);
</script>

<Card.Root>
	<Card.Header>
		<Card.Title class="flex flex-wrap items-center gap-3 text-xl">
			{m.order_title({ number: orderNumber(order.number) })}
			<OrderStatusBadge status={order.status} />
		</Card.Title>
		<Card.Description>{m.order_placed_on({ date: formatDateTime(order.createdAt) })}</Card.Description>
		{#if actions}
			<Card.Action class="flex gap-2">{@render actions()}</Card.Action>
		{/if}
	</Card.Header>
	<Card.Content class="space-y-6">
		<div class="overflow-x-auto rounded-lg border">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>{m.order_col_product()}</Table.Head>
						<Table.Head class="text-end">{m.order_col_price()}</Table.Head>
						<Table.Head class="text-end">{m.order_col_quantity()}</Table.Head>
						<Table.Head class="text-end">{m.orders_col_total()}</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each order.items as item, index (index)}
						{@const href = item.productId ? productHref(item.productSlug, item.productId) : null}
						<Table.Row>
							<Table.Cell class="whitespace-normal">
								{#if href}
									<a {href} class="font-medium hover:underline">{item.productName}</a>
								{:else}
									<span class="font-medium">{item.productName}</span>
								{/if}
								{#if item.sku}
									<span class="block text-xs text-muted-foreground" dir="ltr">{item.sku}</span>
								{/if}
							</Table.Cell>
							<Table.Cell class="text-end tabular-nums">{formatMoney(item.unitPriceCents, order.currency)}</Table.Cell>
							<Table.Cell class="text-end tabular-nums">{item.quantity}</Table.Cell>
							<Table.Cell class="text-end font-medium tabular-nums">
								{formatMoney(item.lineTotalCents, order.currency)}
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>

		<div class="grid gap-6 sm:grid-cols-2">
			<div class="space-y-4 text-sm">
				<div>
					<h3 class="mb-1 font-medium">{m.checkout_shipping()}</h3>
					<address class="leading-relaxed text-muted-foreground not-italic">
						{address.fullName}<br />
						{address.line1}<br />
						{#if address.line2}{address.line2}<br />{/if}
						{address.postalCode}
						{address.city}<br />
						{address.country}
						{#if address.phone}<br /><span dir="ltr">{address.phone}</span>{/if}
					</address>
				</div>
				<div>
					<h3 class="mb-1 font-medium">{m.checkout_payment()}</h3>
					<p class="text-muted-foreground">{paymentMethodLabel(order.paymentMethod)}</p>
				</div>
				{#if order.note}
					<div>
						<h3 class="mb-1 font-medium">{m.checkout_note()}</h3>
						<p class="whitespace-pre-line text-muted-foreground">{order.note}</p>
					</div>
				{/if}
			</div>
			<div class="sm:ps-6">
				<OrderTotals
					subtotalCents={order.subtotalCents}
					shippingCents={order.shippingCents}
					totalCents={order.totalCents}
					currency={order.currency}
				/>
			</div>
		</div>
	</Card.Content>
</Card.Root>
