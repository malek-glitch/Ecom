<script lang="ts">
	import type { components } from '$lib/api/schema';
	import { Badge } from '$lib/components/ui/badge';
	import { m } from '$lib/paraglide/messages';

	type Status = components['schemas']['InventoryRow']['status'];

	let { status, stock }: { status: Status; stock?: number } = $props();

	const variants: Record<Status, 'default' | 'secondary' | 'outline' | 'destructive'> = {
		IN_STOCK: 'outline',
		LOW: 'secondary',
		OUT: 'destructive',
		UNTRACKED: 'outline',
	};
	const labels: Record<Status, () => string> = {
		IN_STOCK: m.stock_status_in_stock,
		LOW: m.stock_status_low,
		OUT: m.shop_out_of_stock,
		UNTRACKED: m.stock_status_untracked,
	};
</script>

<Badge
	variant={variants[status]}
	class={status === 'LOW' ? 'bg-amber-100 text-amber-900 dark:bg-amber-500/20 dark:text-amber-200' : ''}
>
	{labels[status]()}{#if status === 'LOW' && stock !== undefined}&nbsp;· {stock}{/if}
</Badge>
