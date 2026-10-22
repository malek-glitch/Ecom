<script lang="ts">
	import { Badge } from '$lib/components/ui/badge';
	import { orderStatusLabel } from '$lib/shop/labels';
	import type { OrderStatus } from '$lib/shop/store-api';

	let { status }: { status: OrderStatus } = $props();

	const variants: Record<OrderStatus, 'default' | 'secondary' | 'outline' | 'destructive'> = {
		PENDING: 'secondary',
		PAID: 'default',
		SHIPPED: 'default',
		DELIVERED: 'outline',
		CANCELLED: 'destructive',
	};
</script>

<Badge variant={variants[status]}>{orderStatusLabel(status)}</Badge>
