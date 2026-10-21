<script lang="ts">
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { toast } from 'svelte-sonner';
	import { goto, invalidate } from '$app/navigation';
	import { page } from '$app/state';
	import { api, call } from '$lib/api/client';
	import { ApiError, errorMessage } from '$lib/api/errors';
	import type { components } from '$lib/api/schema';
	import ConfirmDialog from '$lib/components/app/ConfirmDialog.svelte';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { createForm } from '$lib/forms/form.svelte';
	import { formatDateTime, formatRelative } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';
	import { shopSettings } from '$lib/shop/store-api';
	import ProductFields from './ProductFields.svelte';
	import ProductImageEditor from './ProductImageEditor.svelte';
	import { productSchema, productValues, toRequest, type AdminProduct } from './product-schema';

	/** Render inside {#key product.id}. */
	let {
		product,
		categories,
		storageEnabled,
	}: { product: AdminProduct; categories: components['schemas']['CategoryResponse'][]; storageEnabled: boolean } =
		$props();

	const currency = shopSettings(page.data.config?.settings).currency;
	let stale = $state(false);
	let confirmingDelete = $state(false);

	const form = createForm({
		schema: productSchema(currency),
		initial: () => productValues(product, currency),
		onSubmit: async (values) => {
			try {
				await call(
					api.PUT('/api/v1/admin/shop/products/{id}', {
						params: { path: { id: product.id } },
						body: { ...toRequest(values, currency), version: product.version },
					}),
				);
			} catch (error) {
				if (error instanceof ApiError && error.code === 'conflict.stale') stale = true;
				throw error;
			}
			toast.success(m.product_saved());
			await invalidate('app:admin-product');
		},
	});

	async function reload() {
		await invalidate('app:admin-product');
		form.reset();
		stale = false;
	}

	async function remove() {
		try {
			await call(api.DELETE('/api/v1/admin/shop/products/{id}', { params: { path: { id: product.id } } }));
		} catch (error) {
			toast.error(errorMessage(error));
			throw error;
		}
		toast.success(m.product_deleted());
		await goto('/admin/products', { replaceState: true });
	}
</script>

<div class={storageEnabled ? 'grid gap-6 lg:grid-cols-[1fr_18rem]' : 'max-w-3xl'}>
	<Card.Root>
		<Card.Header>
			<Card.Title class="text-xl break-words">{form.values.name || product.name}</Card.Title>
			<Card.Description title={formatDateTime(product.updatedAt)}>
				{m.common_updated({ time: formatRelative(product.updatedAt) })}
			</Card.Description>
			<Card.Action>
				<Button variant="ghost" size="icon" aria-label={m.common_delete()} onclick={() => (confirmingDelete = true)}>
					<Trash2 />
				</Button>
			</Card.Action>
		</Card.Header>
		<Card.Content>
			<form onsubmit={form.submit} novalidate class="space-y-6">
				<FormAlert message={form.formError} />
				{#if stale}
					<Button type="button" variant="outline" size="sm" onclick={reload}>{m.common_reload()}</Button>
				{/if}
				<ProductFields {form} {categories} {currency} />
				<div class="flex justify-end">
					<SubmitButton busy={form.submitting} disabled={stale}>{m.common_save()}</SubmitButton>
				</div>
			</form>
		</Card.Content>
	</Card.Root>

	{#if storageEnabled}
		<div>
			<ProductImageEditor {product} onChange={() => invalidate('app:admin-product')} />
		</div>
	{/if}
</div>

<ConfirmDialog
	bind:open={confirmingDelete}
	title={m.product_delete_title()}
	description={m.product_delete_body()}
	confirmLabel={m.common_delete()}
	onConfirm={remove}
/>
