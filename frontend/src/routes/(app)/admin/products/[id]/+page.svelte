<script lang="ts">
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import ExternalLink from '@lucide/svelte/icons/external-link';
	import Seo from '$lib/components/app/Seo.svelte';
	import ProductEditor from '$lib/components/shop/admin/ProductEditor.svelte';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';

	let { data } = $props();
</script>

<Seo title={data.product.name} />

<div class="mb-4 flex flex-wrap items-center justify-between gap-2">
	<Button variant="ghost" size="sm" href="/admin/products" class="-ms-2">
		<ArrowLeft class="rtl:rotate-180" />
		{m.admin_products_back()}
	</Button>
	{#if data.product.active}
		<Button variant="outline" size="sm" href="/products/{data.product.slug}" target="_blank" rel="noopener">
			{m.product_view_in_store()}<ExternalLink />
		</Button>
	{/if}
</div>
{#key data.product.id}
	<ProductEditor product={data.product} categories={data.categories} storageEnabled={data.config.storageEnabled} />
{/key}
