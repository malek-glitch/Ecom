<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import { toast } from 'svelte-sonner';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { api, call } from '$lib/api/client';
	import FormAlert from '$lib/components/app/FormAlert.svelte';
	import PageHeader from '$lib/components/app/PageHeader.svelte';
	import Seo from '$lib/components/app/Seo.svelte';
	import SubmitButton from '$lib/components/app/SubmitButton.svelte';
	import ProductFields from '$lib/components/shop/admin/ProductFields.svelte';
	import { emptyProduct, productSchema, toRequest } from '$lib/components/shop/admin/product-schema';
	import { Button } from '$lib/components/ui/button';
	import * as Card from '$lib/components/ui/card';
	import { createForm } from '$lib/forms/form.svelte';
	import { m } from '$lib/paraglide/messages';
	import { shopSettings } from '$lib/shop/store-api';

	let { data } = $props();

	// Read once: the form's price rules depend on the currency's decimals.
	const currency = shopSettings(page.data.config?.settings).currency;

	const form = createForm({
		schema: productSchema(currency),
		initial: emptyProduct,
		onSubmit: async (values) => {
			const product = await call(api.POST('/api/v1/admin/shop/products', { body: toRequest(values, currency) }));
			toast.success(m.product_created());
			// The edit page is where the photo is added.
			await goto(`/admin/products/${product.id}`, { replaceState: true });
		},
	});
</script>

<Seo title={m.admin_product_new()} />

<div class="mx-auto max-w-3xl">
	<Button variant="ghost" size="sm" href="/admin/products" class="-ms-2 mb-4">
		<ArrowLeft class="rtl:rotate-180" />
		{m.admin_products_back()}
	</Button>
	<PageHeader title={m.admin_product_new()} />
	<Card.Root>
		<Card.Content>
			<form onsubmit={form.submit} novalidate class="space-y-6">
				<FormAlert message={form.formError} />
				<ProductFields {form} categories={data.categories} {currency} idPrefix="new-product" />
				<div class="flex justify-end gap-2">
					<Button variant="outline" href="/admin/products">{m.common_cancel()}</Button>
					<SubmitButton busy={form.submitting}>{m.common_create()}</SubmitButton>
				</div>
			</form>
		</Card.Content>
	</Card.Root>
</div>
