<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import { m } from '$lib/paraglide/messages';

	/** [stock] null: not tracked, always available. [lowStock] comes from the API (per-product threshold). */
	let {
		stock,
		lowStock = false,
		showInStock = false,
	}: { stock: number | null | undefined; lowStock?: boolean; showInStock?: boolean } = $props();
</script>

{#if stock != null && stock <= 0}
	<Badge variant="destructive">{m.shop_out_of_stock()}</Badge>
{:else if stock != null && lowStock}
	<Badge variant="secondary">{m.shop_low_stock({ count: stock })}</Badge>
{:else if showInStock}
	<Badge variant="outline">{m.shop_in_stock()}</Badge>
{/if}
