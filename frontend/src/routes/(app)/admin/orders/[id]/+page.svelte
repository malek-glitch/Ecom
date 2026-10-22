<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import Mail from '@lucide/svelte/icons/mail';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import OrderDetails from '$lib/components/shop/OrderDetails.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Spinner } from '$lib/components/ui/spinner';
	import { m } from '$lib/paraglide/messages';
	import { orderNumber, orderStatusLabel } from '$lib/shop/labels';
	import type { OrderStatus } from '$lib/shop/store-api';

	let { data } = $props();

	let busy = $state<OrderStatus | null>(null);
	let confirmingCancel = $state(false);

	const forward = $derived(data.order.nextStatuses.filter((status) => status !== 'CANCELLED'));
	const canCancel = $derived(data.order.nextStatuses.includes('CANCELLED'));

	async function move(status: OrderStatus) {
		busy = status;
		try {
			await call(
				api.PUT('/api/v1/admin/shop/orders/{id}/status', { params: { path: { id: data.order.id } }, body: { status } }),
			);
			toast.success(m.admin_order_status_changed({ status: orderStatusLabel(status) }));
			await invalidate('app:admin-order');
		} catch (error) {
			toast.error(errorMessage(error));
			throw error;
		} finally {
			busy = null;
		}
	}
</script>

<Seo title={m.order_title({ number: orderNumber(data.order.number) })} />

<div class="mx-auto max-w-5xl space-y-6">
	<Button variant="ghost" size="sm" href="/admin/orders" class="-ms-2">
		<ArrowLeft class="rtl:rotate-180" />
		{m.admin_orders_back()}
	</Button>

	{#if forward.length || canCancel}
		<Card.Root>
			<Card.Header>
				<Card.Title>{m.admin_order_next_step()}</Card.Title>
				<Card.Description>{m.admin_order_next_step_hint()}</Card.Description>
			</Card.Header>
			<Card.Content class="flex flex-wrap gap-2">
				{#each forward as status (status)}
					<Button disabled={busy !== null} onclick={() => move(status).catch(() => undefined)}>
						{#if busy === status}<Spinner />{/if}
						{m.admin_order_mark_as({ status: orderStatusLabel(status) })}
					</Button>
				{/each}
				{#if canCancel}
					<Button variant="outline" disabled={busy !== null} onclick={() => (confirmingCancel = true)}>
						{m.order_cancel()}
					</Button>
				{/if}
			</Card.Content>
		</Card.Root>
	{/if}

	<OrderDetails
		order={data.order}
		productHref={(_slug, productId) => (productId ? `/admin/products/${productId}` : null)}
	>
		{#snippet actions()}
			<Button variant="outline" size="sm" href="mailto:{data.order.email}"
				><Mail />{m.admin_order_email_customer()}</Button
			>
		{/snippet}
	</OrderDetails>
</div>

<ConfirmDialog
	bind:open={confirmingCancel}
	title={m.order_cancel_title()}
	description={m.admin_order_cancel_body()}
	confirmLabel={m.order_cancel()}
	onConfirm={() => move('CANCELLED')}
/>
