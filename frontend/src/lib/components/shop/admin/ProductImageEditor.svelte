<script lang="ts">
	import ImageUp from '@lucide/svelte/icons/image-up';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { toast } from 'svelte-sonner';
	import { api, call } from '$lib/api/client';
	import { errorMessage } from '$lib/api/errors';
	import ProductImage from '$lib/components/shop/ProductImage.svelte';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { Spinner } from '$lib/components/ui/spinner';
	import { m } from '$lib/paraglide/messages';
	import type { AdminProduct } from './product-schema';

	/** The product photo (only when STORAGE_ENABLED=true). `onChange` receives the updated product. */
	let { product, onChange }: { product: AdminProduct; onChange: (product: AdminProduct) => void } = $props();

	const MAX_BYTES = 8 * 1024 * 1024; // the backend's app.storage.max-product-image-size
	let input = $state<HTMLInputElement>();
	let busy = $state(false);

	async function upload(event: Event & { currentTarget: HTMLInputElement }) {
		const file = event.currentTarget.files?.[0];
		event.currentTarget.value = '';
		if (!file) return;
		if (file.size > MAX_BYTES) {
			toast.error(m.error_file_too_large());
			return;
		}
		const body = new FormData();
		body.append('file', file);
		busy = true;
		try {
			// FormData passes through openapi-fetch untouched; the browser sets the multipart boundary.
			const updated = await call(
				api.POST('/api/v1/admin/shop/products/{id}/image', {
					params: { path: { id: product.id } },
					body: body as unknown as { file: string },
				}),
			);
			onChange(updated);
			toast.success(m.product_image_saved());
		} catch (error) {
			toast.error(errorMessage(error));
		} finally {
			busy = false;
		}
	}

	async function remove() {
		busy = true;
		try {
			onChange(
				await call(api.DELETE('/api/v1/admin/shop/products/{id}/image', { params: { path: { id: product.id } } })),
			);
			toast.success(m.product_image_removed());
		} catch (error) {
			toast.error(errorMessage(error));
		} finally {
			busy = false;
		}
	}
</script>

<Card.Root>
	<Card.Header>
		<Card.Title>{m.product_image()}</Card.Title>
		<Card.Description>{m.product_image_hint()}</Card.Description>
	</Card.Header>
	<Card.Content class="space-y-4">
		<ProductImage src={product.imageUrl} alt={product.name} class="aspect-square w-full rounded-lg border" />
		<input
			bind:this={input}
			type="file"
			accept="image/jpeg,image/png,image/webp,image/gif"
			class="hidden"
			onchange={upload}
		/>
		<div class="flex flex-wrap gap-2">
			<Button variant="outline" size="sm" disabled={busy} onclick={() => input?.click()}>
				{#if busy}<Spinner />{:else}<ImageUp />{/if}
				{product.imageUrl ? m.product_image_replace() : m.product_image_upload()}
			</Button>
			{#if product.imageUrl}
				<Button variant="ghost" size="sm" disabled={busy} onclick={remove}><Trash2 />{m.common_delete()}</Button>
			{/if}
		</div>
	</Card.Content>
</Card.Root>
