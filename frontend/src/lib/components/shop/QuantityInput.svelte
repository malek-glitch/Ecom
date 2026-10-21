<script lang="ts">
	import Minus from '@lucide/svelte/icons/minus';
	import Plus from '@lucide/svelte/icons/plus';
	import { Button } from '$lib/components/ui/button';
	import { m } from '$lib/paraglide/messages';

	interface Props {
		value: number;
		min?: number;
		max?: number;
		label: string;
		disabled?: boolean;
		onchange?: (value: number) => void;
	}

	let { value = $bindable(), min = 1, max = 99, label, disabled = false, onchange }: Props = $props();

	function set(next: number) {
		const clamped = Math.min(max, Math.max(min, Number.isFinite(next) ? Math.floor(next) : min));
		value = clamped;
		onchange?.(clamped);
	}
</script>

<div class="inline-flex items-center rounded-md border" role="group" aria-label={label}>
	<Button
		type="button"
		variant="ghost"
		size="icon"
		class="size-8 rounded-e-none"
		aria-label={m.cart_decrease()}
		disabled={disabled || value <= min}
		onclick={() => set(value - 1)}
	>
		<Minus />
	</Button>
	<input
		type="number"
		inputmode="numeric"
		class="h-8 w-10 [appearance:textfield] bg-transparent text-center text-sm tabular-nums outline-none [&::-webkit-inner-spin-button]:appearance-none"
		aria-label={label}
		{min}
		{max}
		{disabled}
		{value}
		onchange={(event) => set(Number(event.currentTarget.value))}
	/>
	<Button
		type="button"
		variant="ghost"
		size="icon"
		class="size-8 rounded-s-none"
		aria-label={m.cart_increase()}
		disabled={disabled || value >= max}
		onclick={() => set(value + 1)}
	>
		<Plus />
	</Button>
</div>
