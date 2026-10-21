<script lang="ts">
	import { Separator } from '$lib/components/ui/separator';
	import { formatMoney } from '$lib/i18n/format';
	import { m } from '$lib/paraglide/messages';

	let {
		subtotalCents,
		shippingCents,
		totalCents,
		currency,
	}: { subtotalCents: number; shippingCents: number; totalCents: number; currency: string } = $props();
</script>

<dl class="space-y-2 text-sm">
	<div class="flex justify-between gap-4">
		<dt class="text-muted-foreground">{m.cart_subtotal()}</dt>
		<dd class="tabular-nums">{formatMoney(subtotalCents, currency)}</dd>
	</div>
	<div class="flex justify-between gap-4">
		<dt class="text-muted-foreground">{m.cart_shipping()}</dt>
		<dd class="tabular-nums">{shippingCents === 0 ? m.cart_shipping_free() : formatMoney(shippingCents, currency)}</dd>
	</div>
	<Separator />
	<div class="flex justify-between gap-4 text-base font-semibold">
		<dt>{m.cart_total()}</dt>
		<dd class="tabular-nums">{formatMoney(totalCents, currency)}</dd>
	</div>
</dl>
