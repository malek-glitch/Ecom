<script lang="ts">
	import ShoppingBag from '@lucide/svelte/icons/shopping-bag';
	import { onMount } from 'svelte';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';
	import { cart } from '$lib/shop/cart.svelte';

	onMount(() => cart.load());
</script>

<Button href="/cart" variant="ghost" size="icon" class="relative" aria-label={m.cart_open({ count: cart.count })}>
	<ShoppingBag />
	{#if cart.count > 0}
		<span
			class="absolute -end-0.5 -top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-primary px-1 text-[10px] leading-none font-semibold text-primary-foreground tabular-nums"
			aria-hidden="true"
		>
			{cart.count > 99 ? '99+' : cart.count}
		</span>
	{/if}
</Button>
