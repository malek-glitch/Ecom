<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import * as Table from '$lib/components/ui/table';
	import { formatDateTime, formatRelative } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { orderNumber } from '$lib/shop/labels';
	import { cn } from '$lib/utils';
	import { MOVEMENT_LABELS, type Movement } from './types';

	/** Stock history rows. [showProduct]: add a product column (store-wide history). */
	let { movements, showProduct = false }: { movements: Movement[]; showProduct?: boolean } = $props();
</script>

<Table.Root>
	<Table.Header>
		<Table.Row>
			<Table.Head>{m.movement_col_when()}</Table.Head>
			{#if showProduct}<Table.Head>{m.order_col_product()}</Table.Head>{/if}
			<Table.Head>{m.movement_col_type()}</Table.Head>
			<Table.Head class="text-end">{m.movement_col_change()}</Table.Head>
			<Table.Head class="text-end">{m.movement_col_after()}</Table.Head>
			<Table.Head class="hidden md:table-cell">{m.movement_col_details()}</Table.Head>
		</Table.Row>
	</Table.Header>
	<Table.Body>
		{#each movements as movement (movement.id)}
			<Table.Row>
				<Table.Cell class="whitespace-nowrap text-muted-foreground" title={formatDateTime(movement.createdAt)}>
					{formatRelative(movement.createdAt)}
				</Table.Cell>
				{#if showProduct}
					<Table.Cell class="max-w-48 truncate">
						{#if movement.productName}
							<a href="/admin/inventory/{movement.productId}" class="hover:underline">{movement.productName}</a>
						{:else}
							<span class="text-muted-foreground">{m.inventory_deleted_product()}</span>
						{/if}
					</Table.Cell>
				{/if}
				<Table.Cell><Badge variant="outline">{MOVEMENT_LABELS[movement.type]()}</Badge></Table.Cell>
				<Table.Cell
					class={cn(
						'text-end font-medium tabular-nums',
						movement.quantity > 0 && 'text-emerald-700 dark:text-emerald-400',
						movement.quantity < 0 && 'text-destructive',
					)}
					dir="ltr"
				>
					{movement.quantity > 0 ? '+' : ''}{movement.quantity}
				</Table.Cell>
				<Table.Cell class="text-end tabular-nums">{movement.stockAfter}</Table.Cell>
				<Table.Cell class="hidden max-w-72 text-sm text-muted-foreground md:table-cell">
					<span class="line-clamp-2" dir="auto">
						{#if movement.orderId && movement.orderNumber}
							<a href="/admin/orders/{movement.orderId}" class="text-foreground hover:underline">
								{m.order_title({ number: orderNumber(movement.orderNumber) })}
							</a>
						{:else if movement.orderNumber}
							{m.order_title({ number: orderNumber(movement.orderNumber) })}
						{/if}
						{movement.note}
						{#if movement.actorName}<span class="text-xs">· {movement.actorName}</span>{/if}
					</span>
				</Table.Cell>
			</Table.Row>
		{:else}
			<Table.Row>
				<Table.Cell colspan={showProduct ? 6 : 5} class="h-20 text-center text-muted-foreground">
					{m.inventory_no_movements()}
				</Table.Cell>
			</Table.Row>
		{/each}
	</Table.Body>
</Table.Root>
