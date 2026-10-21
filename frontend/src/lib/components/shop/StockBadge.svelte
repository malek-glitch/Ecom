<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import { m } from '$lib/paraglide/messages';
	import { LOW_STOCK } from '$lib/shop/store-api';

	let { stock, showInStock = false }: { stock: number; showInStock?: boolean } = $props();
</script>

{#if stock <= 0}
	<Badge variant="destructive">{m.shop_out_of_stock()}</Badge>
{:else if stock <= LOW_STOCK}
	<Badge variant="secondary">{m.shop_low_stock({ count: stock })}</Badge>
{:else if showInStock}
	<Badge variant="outline">{m.shop_in_stock()}</Badge>
{/if}
