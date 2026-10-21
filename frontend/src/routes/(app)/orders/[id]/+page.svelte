<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import Landmark from '@lucide/svelte/icons/landmark';
	import { toast } from 'svelte-sonner';
	import { invalidate } from '$app/navigation';
	import { page } from '$app/state';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import OrderDetails from '$lib/components/shop/OrderDetails.svelte';
	import * as Alert from '$lib/components/ui/alert';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';
	import { orderNumber } from '$lib/shop/labels';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	let confirmingCancel = $state(false);
	const justPlaced = $derived(page.url.searchParams.has('placed'));
	const bankDetails = $derived(shopSettings(data.config.settings).bankTransferInstructions);

	async function cancel() {
		try {
			await call(api.POST('/api/v1/orders/{id}/cancel', { params: { path: { id: data.order.id } } }));
		} catch (error) {
			toast.error(errorMessage(error));
			throw error;
		}
		toast.success(m.order_cancelled_toast());
		await invalidate('app:order');
	}
</script>

<Seo title={m.order_title({ number: orderNumber(data.order.number) })} />

<div class="mx-auto max-w-4xl space-y-6">
	<Button variant="ghost" size="sm" href="/orders" class="-ms-2">
		<ArrowLeft class="rtl:rotate-180" />
		{m.orders_back()}
	</Button>

	{#if justPlaced && data.order.status !== 'CANCELLED'}
		<Alert.Root>
			<CircleCheck />
			<Alert.Title>{m.order_placed_title()}</Alert.Title>
			<Alert.Description>{m.order_placed_body({ email: data.order.email })}</Alert.Description>
		</Alert.Root>
	{/if}

	{#if data.order.paymentMethod === 'BANK_TRANSFER' && data.order.status === 'PENDING' && bankDetails}
		<Alert.Root>
			<Landmark />
			<Alert.Title>{m.order_bank_transfer_title()}</Alert.Title>
			<Alert.Description>
				<p>{m.order_bank_transfer_body({ number: orderNumber(data.order.number) })}</p>
				<p class="mt-2 whitespace-pre-line text-foreground">{bankDetails}</p>
			</Alert.Description>
		</Alert.Root>
	{/if}

	<OrderDetails order={data.order}>
		{#snippet actions()}
			{#if data.order.cancellable}
				<Button variant="outline" size="sm" onclick={() => (confirmingCancel = true)}>{m.order_cancel()}</Button>
			{/if}
		{/snippet}
	</OrderDetails>
</div>

<ConfirmDialog
	bind:open={confirmingCancel}
	title={m.order_cancel_title()}
	description={m.order_cancel_body()}
	confirmLabel={m.order_cancel()}
	onConfirm={cancel}
/>
